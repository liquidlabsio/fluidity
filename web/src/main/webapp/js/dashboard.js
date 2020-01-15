/* globals Chart:false, feather:false */

// (function () {
$(document).ready(function () {
  'use strict'

  feather.replace()

  new Logscape.Navigation();

  $(".nav-link.active").trigger("click")

  $("#fileupload").attr("action", LOGSCAPE_URL + '/upload/file')

  // $('#fileupload').fileupload()
  $('#fileupload').fileupload({
    // dataType: 'json',
    done: function (e, data) {
      $.each(data.result.files, function (index, file) {
        $('<p/>').text(file.name).appendTo(document.body);
      });
    }
  });

  //https://github.com/blueimp/jQuery-File-Upload/wiki/Options
  $('#fileupload').fileupload(
  'option',
      {
        paramName: 'fileContent',
        singleFileUploads: true
      })

  $('#fileupload')
      .bind('fileuploadstart', function (e) { alert("File upload started")})
      .bind('fileuploaddone', function (e, data) {alert("File upload done")})
      .bind('fileuploadfail', function (e, data) {alert("File upload failed:" + e)})


});

Logscape.Navigation = function () {
  console.log("Logscape.Navigation created")
  setupNavigationActions();


  function setupNavigationActions() {

    $(".nav-link").click(function (event) {
      $(".nav-link").removeClass("active")
      $(event.currentTarget).addClass("active")

      $(".mainPanelTab").hide()
      $("#"+event.currentTarget.dataset.target).show()
    })


  }
}



