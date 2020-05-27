// define pub-sub topics
Fluidity.Explorer.Topics = {
    uploadFile: 'explorerUploadFile',
    getListFiles: 'explorerGetListFiles',
    setListFiles: 'explorerSetListFiles',
    getFileContent: 'explorerGetFileContent',
    setFileContent: 'explorerSetFileContent',
    importFromStorage: 'importFromStorage',
    importedFromStorage: 'importedFromStorage',
    removeImportFromStorage: 'removeImportFromStorage',
    removedImportFromStorage: 'removedImportFromStorage',

    downloadFileContent: 'explorerDownloadFileContent',

    startSpinner: 'startSpinner',
    stopSpinner: 'stopSpinner',
    setExplorerToOpen: 'setExplorerToOpen'
}

$(document).ready(function () {

    let fileList = new Fluidity.Explorer.FileList($('#explorerFileListTable'));


});

Fluidity.Explorer.FileList = function (table) {
    console.log("Fluidity.Explorer.FileList created")

    let dataTable
    let sources
    bindEditor()

    bindIdsToTable()


    function bindEditor() {

        let explorerEditor = ace.edit("explorerEditor");
        explorerEditor.setTheme("ace/theme/monokai");
        explorerEditor.session.setMode("ace/mode/javascript");
        explorerEditor.session.setUseWrapMode(true);


        $.Topic(Fluidity.Explorer.Topics.setFileContent).subscribe(function(content) {
            $("#explorerOpenFileName").get(0).scrollIntoView();
            explorerEditor.setValue(content)
        })

        $.Topic(Fluidity.Explorer.Topics.setExplorerToOpen).subscribe(function(content) {
            // show this tab
            $(".nav-link.explorer").click()
            $("#explorerOpenFileName").text("Filename: " + content[0]);
            $.Topic(Fluidity.Explorer.Topics.getFileContent).publish(content[0], content[1])
        })

        $('#refreshFiles').click(function(){
            $.Topic(Fluidity.Explorer.Topics.getListFiles).publish("doit")
        });
        $.Topic(Fluidity.Explorer.Topics.getListFiles).publish("get file list on page load")

        $('.zoomExplorer').click(function(event){
                let zoomDirection = $(event.currentTarget).data().zoom;
                let normalClass = "normalSizeEditor";
                let mediumClass = "mediumSizeEditor";
                let largeClass = "largeSizeEditor";
                let editor = $('#explorerEditor')
                if (editor.hasClass(normalClass)) {
                    if (zoomDirection == "in") {
                        editor.removeClass(normalClass)
                        editor.addClass(mediumClass)
                    } else {
                    // already at normal size
                    }
                } else if (editor.hasClass(mediumClass)) {
                    editor.removeClass(mediumClass)
                    if (zoomDirection == "in") {
                        editor.addClass(largeClass)
                    } else {
                        editor.addClass(normalClass)
                    }
                } else if (editor.hasClass(largeClass)) {
                  if (zoomDirection == "in") {
                  } else {
                     editor.removeClass(largeClass)
                      editor.addClass(mediumClass)
                  }
                }
                explorerEditor.resize()
                $("#explorerOpenFileName").get(0).scrollIntoView();
                return false;
        })
    }
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

    $('#explorerFileListTable').on('click','td', function (event) {
        try {
            let filename = dataTable.api().row( this ).data().filename;
            let cell = dataTable.api().cell( this )
            let action = $(event.target).data().action

            if (action == "view") {
                $("#explorerOpenFileName").text("Filename: " + filename)
                $.Topic(Fluidity.Explorer.Topics.getFileContent).publish(filename, 0)
            } else if (action == "download"){
                $.Topic(Fluidity.Explorer.Topics.downloadFileContent).publish(filename)
            } else {
                $("#explorerOpenFileName").text("Filename: " + filename)
                $.Topic(Fluidity.Explorer.Topics.getFileContent).publish(filename, 0)
            }
        } catch (err) {
            console.log(err.stack)
        }
        return false;
    })

    $.Topic(Fluidity.Explorer.Topics.setListFiles).subscribe(function (listing) {
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
                    "<a class='fas fa-eye btn btn-link explorerFileActions view' data-action='view' data-filename='" + item.filename + "' href='#' title='View'></a>"+
                    "<a class='fas fa-search btn btn-link explorerFileActions' data-action='search' data-filename='" + item.filename + "' href='#' title='Search against this'></a>"+
                    "<a class='fas fa-times btn btn-link explorerFileActions' data-action='delete' data-filename='" + item.filename + "' href='#' title='Delete'></a>"+
                    "<a class='fas fa-cloud-download-alt btn btn-link explorerFileActions download' data-action='download' data-filename='" + item.filename + "' href='#' title='Download'></a> "
            })
            dataTable.fnAddData(listing)
            sources = listing.files
        }
    }

    function refreshIt() {
        $.Topic(Fluidity.Explorer.Topics).publish("")
    }

    return {
        refresh: function () {
            refreshIt()
        }
    }

}