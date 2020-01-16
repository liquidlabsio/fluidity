/* globals Chart:false, feather:false */

// (function () {
$(document).ready(function () {
  'use strict'

  $("#fileupload").attr("data-url", LOGSCAPE_URL + '/upload/file')
  $("#fileupload").fileupload({
    dataType: "json",
    add: function(e, data) {
      data.formData = {
                filename: data.files[0].name,
                toTime: data.files[0].lastModified,
                tenant: $("#uploadResourceInput").val(),
                resource: $("#uploadResourceInput").val(),
                tags: $("#uploadTagsInput").val()
                }
      if (data.formData.resource.length == 0 || data.formData.tags.length == 0) {
        alert("Resource value must be specified; try again")
        return false;
      }
      data.context = $('<p class="file">')
        .append($('<a target="_blank">').text(data.files[0].name + " bytes:" + data.files[0].size))
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


