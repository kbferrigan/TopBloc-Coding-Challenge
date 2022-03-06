//Submission by Kyle Ferrigan

async function getLowStockItems() {//asych due to fetch request
    console.log("Attempting to fetch low-stock items") //DEBUG

    //Fetch request
    let res = await fetch('http://localhost:4567/low-stock', {method: 'GET'})
    let data = await res.text()
    let pData = JSON.parse(data);
    //console.log(pData); //DEBUG

    //Output parsed json data to frontend
    var table = document.getElementById("stockTable").getElementsByTagName('tbody')[0];
    table.innerHTML = "";//clear table in case get low stock button is pressed more than once
    for(var i = 0; pData.at(i) != null; i++){ //Make table and insert data
        var row = table.insertRow();
        var column0 = row.insertCell(0);
        var column1 = row.insertCell(1);
        var column2 = row.insertCell(2);
        var column3 = row.insertCell(3);
        var column4 = row.insertCell(4);
        column0.innerHTML = '<text id = sku'+ i +'>' + pData.at(i).id + '</text>' //added id to be able to track later
        column1.innerText = pData.at(i).name;
        column2.innerText = pData.at(i).stock;
        column3.innerText = pData.at(i).capacity;
        column4.innerHTML = '<input type="text" id="fname'+ i +'" name="fname">'; //insert textbox for data entry unique id for easier extraction in re-order cost
    }
}
async function getReorderCost(){ //async due to fetch request
    console.log("Attempting to fetch restock-cost")

    //Map skus to amount ordered
    let skumap = new Map();
    for(let i = 0; document.getElementById("sku"+i.toString()) != null; i++){ //keep iterating through all textboxes
        //console.log(document.getElementById("sku"+i.toString()).innerText + " : " + document.getElementById("fname"+i.toString()).value); //DEBUG
        skumap.set(document.getElementById("sku"+i.toString()).innerText, document.getElementById("fname"+i.toString()).value); //Get sku and value from textboxes and map them together
    }


    //Convert from map to JSON
    var obj =  Object.fromEntries(skumap);
    var json = JSON.stringify(obj);


    //Fetch request to backend
    let res = await fetch('http://localhost:4567/restock-cost',{
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: json
    });
    let data = await res.text()
    //console.log(data) DEBUG


    //Output Data to Frontend
    if (!isNaN(Number(data))){//Determine if data is a number
        document.getElementById("totalCost").innerText = "Total Cost: $" +  Number(data).toFixed(2);//Cutoff at two decimal points for cents
    }else{//if not display message by itself
        document.getElementById("totalCost").innerText = data;
    }
}

export default function Challenge() {
  return (
    <>
      <table id="stockTable">
        <thead>
          <tr>
            <td>SKU</td>
            <td>Item Name</td>
            <td>Amount in Stock</td>
            <td>Capacity</td>
            <td>Order Amount</td>
          </tr>
        </thead>
        <tbody>
          {/* 
          Create an <ItemRow /> component that's rendered for every inventory item. The component
          will need an input element in the Order Amount column that will take in the order amount and 
          update the application state appropriately.
          */}
        </tbody>
      </table>
      {/* Display total cost returned from the server */}
      <div id = "totalCost">Total Cost: </div>
      {/* Add event handlers to these buttons that use the Java API to perform their relative actions. */}
      <button onClick={getLowStockItems}>Get Low-Stock Items</button>
      <button onClick={getReorderCost}>Determine Re-Order Cost</button>
    </>
  );
}
