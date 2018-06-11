(function () {
    
    
    setInterval(update, 5000);
    
    update();
    
    
    $("#deleter").on('click', deleteData);
    
   
    function update (){
        $.ajax({
            url: "AdminService/api/data/count",
            success: updateCounter
        });
        $.ajax({
            url: "AdminService/api/data/trace",
            success: updateTrace
        });
        $.ajax({
            url: "AdminService/api/overview/all",
            success: updateStats
        });
    }
    
    
    function updateCounter (result) {
        if (result.success) {
            $("#counterb").html(result.data.reduce(function (a, b) {
                return a + "<tr><td>"+ b.client +"</td><td>"+ b.count +"</td></tr>";
            }, ""));
        }
    }
    
    
    function updateTrace (result) {
        if (result.success) {
            $("#traceb").html(result.data.reduce(function (a, b) {
                return a + "<tr><td>"+ b.client +"</td><td>"+ b.server +"</td><td>"+ b.message +"</td></tr>";
            }, ""));
        }
    }
    
    
    function updateStats (result) {
        if (result.success) {
            var tableStr = "";
            for (var key in result.data) {
                tableStr += "<tr><td>"+ key +"</td><td>"+ result.data[key] +"</td></tr>";
            }
            $("#statb").html(tableStr);
        }
    }
    
    
    function deleteData () {
        $.ajax({
            url: "AdminService/api/data",
            type: "DELETE",
            success: function (result) {
                if (result.success) {
                    update();
                    $('.navbar-nav a[href="#home"]').tab('show');
                    
                    var notification = $('#success_remove');
                    notification.fadeIn("200", setTimeout.bind(
                        window,
                        notification.fadeOut.bind(notification, "200"),
                        4000
                    ));
                } else {
                    alert("Failed to remove data!");
                }
            }
        });
    }
})();