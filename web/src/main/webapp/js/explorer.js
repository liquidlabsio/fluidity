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
                    { mData: "to" },
                    { mData: "actions" }
                ]
            })
    }

    table.click(function (event) {
        try {
            let filename = event.target.parentElement.childNodes[0].childNodes[0].nodeValue;

            $("#explorerOpenFileName").text("Filename: " + filename)
            $.Topic(Logscape.Explorer.Topics.getFileContent).publish(filename)
            return false;
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
                if (item.size > 2048) {
                    item.size = Number(item.size/1024).toLocaleString()  + "Kb"
                } else {
                    item.size = Number(item.size).toLocaleString()  + "b"
                }

                item.from = new Date(item.fromTime).toLocaleString();
                item.to =  new Date(item.toTime).toLocaleString();
                item.actions =
                    "<a class='fas fa-eye btn btn-link explorerFileActions' data-filename='" + item.filename + "' href='#' title='View'></a>"+
                    "<a class='fas fa-search btn btn-link explorerFileActions' data-filename='" + item.filename + "' href='#' title='Search against this'></a>"+
                    "<a class='fas fa-times btn btn-link explorerFileActions' data-filename='" + item.filename + "' href='#' title='Delete'></a>"+
                    "<a class='fas fa-cloud-download-alt btn btn-link explorerFileActions' data-filename='" + item.id + "' href='#' title='Download'></a> "
            })
            dataTable.fnAddData(listing)
            sources = listing.files
        }

        $(".explorerFileActions").unbind();
        $(".explorerFileActions").click(function(event){
                    let filename = $(event.currentTarget).data().filename;
                    $("#explorerOpenFileName").text("Filename: " + filename)
                    $.Topic(Logscape.Explorer.Topics.getFileContent).publish(filename)
        });


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