/* globals Chart:false, feather:false */

$(document).ready(function () {
  'use strict'

    $("#dataImportForm").submit(function(event) {
        let inputs = $(this).serializeArray();

        if (inputs[0].value.length == 0 || inputs[1].value.length == 0 || inputs[2].value.length == 0) {
            alert("Please provide values for all 3 inputs")
            return false;
        }
        $.Topic(Precognito.Explorer.Topics.importFromStorage).publish(
                inputs[0].value, inputs[1].value, inputs[2].value
        );
        return false;
    })


    $("#removeFromStorageButton").click(function(event) {
        let inputs = $("#dataImportForm").serializeArray();

        if (inputs[0].value.length == 0 || inputs[1].value.length == 0 || inputs[2].value.length == 0) {
            alert("Please provide values for all 3 inputs")
            return false;
        }
        $.Topic(Precognito.Explorer.Topics.removeImportFromStorage).publish(
                inputs[0].value, inputs[1].value, inputs[2].value
        );

    })

   $.Topic(Precognito.Explorer.Topics.importedFromStorage).subscribe(function(event) {
        alert("Data was imported:" + event)
   })
      $.Topic(Precognito.Explorer.Topics.removedImportFromStorage).subscribe(function(event) {
           alert("Data was un-imported:" + event)
      })


  $("#fileupload").attr("data-url", SERVICE_URL + '/storage/upload')
  $("#fileupload").fileupload({
    dataType: "json",
    add: function(e, data) {
      data.formData = {
                filename: data.files[0].name,
                toTime: data.files[0].lastModified,
                tenant: DEFAULT_TENANT,
                resource: $("#uploadResourceInput").val(),
                tags: $("#uploadTagsInput").val()
                }
      if (data.formData.resource.length == 0 || data.formData.tags.length == 0) {
        alert("Resource identifier must be specified; try again")
        return false;
      }
      data.context = $('<p class="file">')
        .append($('<a target="_blank">').text(data.files[0].name + " bytes:" + Precognito.formatNumber(data.files[0].size)))
        .appendTo($("#files-list"));
      data.submit();
    },
    progress: function(e, data) {
      var progress = parseInt((data.loaded / data.total) * 100, 10);
      data.context.css("background-position-x", 100 - progress + "%");
    },
    // callback not working
    done: function(e, data) {
     data.context.text('Upload finished.');
      data.context
        .addClass("done")
        .find("a")
        .prop("href", data.result.files[0].url);
    }
  });
    $('#fileupload')
    .bind('fileuploaddone', function (e, data) {
    console.log("done!!")
    })
});


