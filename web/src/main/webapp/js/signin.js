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
                   success: function(data)
                   {
                       window.localStorage.setItem("logscape-auth", data)
                       window.location.href = "index.html"
                   },
                   fail: function(data){
                        alert(data);
                   }
             });

            return false;
    })
})