$(document).ready(function () {

    $("#signin").submit(function(event) {
            let inputs = $(this).serializeArray();

            if (inputs[0].value.length == 0 || inputs[1].value.length ==0) {
                alert("Please provide values")
                return false;
            }
            let form = {
                username: inputs[0].value,
                password: inputs[1].value
            }

         $.ajax({
            type: "POST",
            url: SERVICE_URL + '/auth/login',
            data: form, // serializes the form's elements.
            crossDomain: true,
            success: function(data) {
               window.localStorage.setItem("fluidity-auth", data)
               window.location.href = "index.html"
            },
            error: function(data){
                console.log(data)
                alert("Error happened:" + data);
            }

             });

            return false;
    })
})