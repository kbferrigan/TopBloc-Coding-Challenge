//import netscape.javascript.JSObject; Note: Causes issues on MacOS
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ooxml.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.util.*;
import static spark.Spark.*;

public class Main {

    public static void main(String[] args) {

        //This is required to allow GET and POST requests with the header 'content-type'
        options("/*",
                (request, response) -> {
                        response.header("Access-Control-Allow-Headers",
                                "content-type");

                        response.header("Access-Control-Allow-Methods",
                                "GET, POST");


                    return "OK";
                });

        //This is required to allow the React app to communicate with this API
        before((request, response) -> response.header("Access-Control-Allow-Origin", "http://localhost:3000"));

        //Return JSON containing the candies for which the stock is less than 25% of it's capacity
        get("/low-stock", (request, response) -> {
            System.out.println("Fetching low-stock items"); //DEBUG

            JSONArray lowStockList = new JSONArray(); //Container for JSON to be returned

            try{
                FileInputStream file = new FileInputStream(new File("resources/Inventory.xlsx"));
                Workbook workbook = new XSSFWorkbook(file);
                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> iterator = sheet.iterator();
                Row currentRow = iterator.next(); // skip reading the first row which is just the headers

                while (iterator.hasNext()) {
                    currentRow = iterator.next();
                    Iterator<Cell> cellIterator = currentRow.iterator();
                    JSONObject item = new JSONObject(); //Create new JSON object to store item values in

                    for (int i = 0; cellIterator.hasNext(); i++) {
                        Cell currentCell = cellIterator.next();

                        if (currentCell.getCellType() == CellType.STRING) {
                            //System.out.print(currentCell.getStringCellValue() + " "); //console DEBUG
                            item.put("name", currentCell.getStringCellValue());
                        }

                        else if (currentCell.getCellType() == CellType.NUMERIC) {
                            //System.out.print(currentCell.getNumericCellValue() + " "); //console DEBUG
                            switch (i){//Store JSON values
                                case 1: //Stock
                                    item.put("stock", currentCell.getNumericCellValue());
                                    break;

                                case 2: //Capacity
                                    item.put("capacity", currentCell.getNumericCellValue());
                                    break;

                                case 3: //ID
                                    item.put("id", currentCell.getNumericCellValue());
                                    break;
                            }
                        }
                    }

                    if ((double)item.get("stock") < ((double)item.get("capacity")) * 0.25){ //if item stock is less then a quarter capcity add to low stock list
                        lowStockList.add(item);
                    }
                    //System.out.println();//console DEBUG
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return lowStockList;
        });




        //Return JSON containing the total cost of restocking candy
        post("/restock-cost", (request, response) -> {
            System.out.println("Fetching restock cost"); //DEBUG
            double restockCost = 0;
            //System.out.println(request.body()); //DEBUG

            //Get keys(SKU/ID) from POST request
            JSONParser parser = new JSONParser();
            JSONObject jObj = (JSONObject) parser.parse(request.body());
            Set kSet = jObj.keySet();


            //SKU:Price Maps
            Map<Integer,Double> candycorp = new HashMap();
            Map<Integer,Double> theSweetSuite = new HashMap();
            Map<Integer,Double> dentistsHateUs = new HashMap();

            //open Excel and fill out SKU:Price maps
            try{
                FileInputStream file = new FileInputStream(new File("resources/Distributors.xlsx"));
                Workbook workbook = new XSSFWorkbook(file);

                ArrayList<Sheet> wbook = new ArrayList<>(); //make list for easier iteration and reusable code
                wbook.add(workbook.getSheetAt(0));
                wbook.add(workbook.getSheetAt(1));
                wbook.add(workbook.getSheetAt(2));


                for (int i = 0; i<3; i++){
                    Iterator<Row> iterator = wbook.get(i).iterator();
                    Row currentRow = iterator.next(); // skip reading the first row which is just the headers
                    while (iterator.hasNext()) {
                        currentRow = iterator.next();
                        Iterator<Cell> cellIterator = currentRow.iterator();
                        int sku = 0;
                        double cost = 0;
                        for (int j = 0; cellIterator.hasNext(); j++) {
                            Cell currentCell = cellIterator.next();
                            if (currentCell.getCellType() == CellType.NUMERIC) {
                                //System.out.print(currentCell.getNumericCellValue() + " "); //console DEBUG
                                switch (j){//Store JSON values
                                    case 1: //SKU
                                        sku = ((int) currentCell.getNumericCellValue());
                                        break;

                                    case 2: //COST
                                        cost = ((double) currentCell.getNumericCellValue());
                                        break;
                                }
                            }
                        }

                        switch(i){//change what map to put into depending on current workbook
                            case 0:
                                candycorp.put(sku,cost);
                                break;
                            case 1:
                                theSweetSuite.put(sku,cost);
                                break;
                            case 2:
                                dentistsHateUs.put(sku,cost);
                                break;
                        }
                        //System.out.println();//console DEBUG
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }



            for(Object keys : kSet){
                //System.out.println(keys); //DEBUG
                //Use keys to find amount needed here and then find what the lowest cost is
                int sku = Integer.valueOf((String) keys);
                //System.out.println(sku); //DEBUG

                double cc = 999999999;
                double tss = 999999999;
                double dhu = 999999999;

                //Inexpensive way of filtering out null
                if (candycorp.get(sku) == null) {
                    cc = 999999999;
                }else{
                    cc = candycorp.get(sku);
                }

                if (theSweetSuite.get(sku) == null) {
                    tss = 999999999;
                }else{
                    tss = theSweetSuite.get(sku);
                }

                if (dentistsHateUs.get(sku) == null) {
                    dhu = 999999999;
                }else {
                    dhu = dentistsHateUs.get(sku);
                }
                try{//add the minimum cost item times the amount of the item, round to nearest 100th
                    restockCost += (Integer.parseInt((String)jObj.get(Integer.toString(sku))) * Math.min(cc,Math.min(tss,dhu)));
                }catch (Exception NumberFormatException){
                    return "ERROR: Please enter integers into all fields";
                }


            }
            //System.out.println(restockCost); //DEBUG
            return restockCost;
        });

    }
}
