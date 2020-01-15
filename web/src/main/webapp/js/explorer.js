// define pub-sub topics
Logscape.Explorer.Topics = {
    uploadFile: 'explorerUploadFile',
    getListFiles: 'explorerGetListFiles',
    setListFiles: 'explorerSetListFiles',
    getFileContent: 'explorerGetFileContent',
    setFileContent: 'explorerSetFileContent'
}

$(document).ready(function () {

    let fileList = new Logscape.Explorer.FileList($('#explorerFileListTable'));

    let editor = ace.edit("explorerEditor");
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode("ace/mode/javascript");


    $.Topic(Logscape.Explorer.Topics.setFileContent).subscribe(function(event) {
        editor.setValue(event.fileContent)
    })


    $('#refreshFiles').click(function(){
        $.Topic(Logscape.Explorer.Topics.getListFiles).publish("doit")
    });
    $.Topic(Logscape.Explorer.Topics.getListFiles).publish("get file list on page load")
});

Logscape.Explorer.FileList = function (table) {
    console.log("Logscape.Explorer.FileList created")

    let dataTable
    let sources

    bindIdsToTable()

    function bindIdsToTable() {

        dataTable = table.dataTable(
            {
                bLengthChange: false,
                sPaginationType: "full_numbers",
                iDisplayLength: 20,
                aoColumns: [
                    { mData: "filename", bVisible: true },
                    { mData: "tags" },
                    { mData: "resource" },
                    { mData: "size" },
                    { mData: "from" },
                    { mData: "to" }
                ]
            })
    }

    table.click(function (event) {
        try {
            let filename = event.target.parentElement.childNodes[0].childNodes[0].nodeValue;

            $("#explorerOpenFileName").text("Filename: " + filename)
            $.Topic(Logscape.Explorer.Topics.getFileContent).publish(filename)
        } catch (err) {
            console.log(err.stack)
        }
    })

    $.Topic(Logscape.Explorer.Topics.setListFiles).subscribe(function (listing) {
        setListing(listing)
    })
    function setListing(listing) {
        dataTable.fnClearTable()
        if (listing.length >0) {
            jQuery.each(listing, function (i, item) {
                item.volume = 0
                if (item.sizeBytes > 2048) {
                    item.size = Number(item.sizeBytes/1024).toLocaleString()  + "Kb"
                } else {
                    item.size = Number(item.sizeBytes).toLocaleString()  + "b"
                }

                item.from = new Date(item.fromTime).toLocaleString();
                item.to =  new Date(item.toTime).toLocaleString();
                // item.actions = "<a class='ds_search fa fa-search btn btn-link' dsid='" + item.id + "' href='#' title='Search against this'></a> <a class='ds_remove fa fa-times btn btn-link' dsid='" + item.id + "' href='#' title='Delete'></a><a class='ds_reindex fa fa-repeat btn btn-link ' dsid='" + item.id + "' href='#' title='ReIndex'></a> "
            })
            dataTable.fnAddData(listing)
            sources = listing.files
        }

    }

    function refreshIt() {
        $.Topic(Logscape.Explorer.Topics).publish("")
    }

    return {
        refresh: function () {
            refreshIt()
        }
    }

}