
LOGSCAPE_URL = 'http://0.0.0.0:8080'
KEY = '5b578yg9yvi8sogirbvegoiufg9v9g579gviuiub8' // not real

$(document).ready(function () {
    binding()
});

class FilesInterface {

    upload(file) {
        throw new err ("not implemented")
    }

    listFiles() {
        throw new err ("not implemented")
    }

    fileContents(filename) {
        throw new err ("not implemented")
    }
}

var testFiles = [
    { filename:'/this/is/a/file.txt', tags:'prod, uk', resource:'someHost', sizeBytes:100, from:'10:00.21', to:'14:02.34'},
    { filename:'/this/is/a/fileB.txt', tags:'prod, uk', resource:'someHost', sizeBytes:100, from:'10:13.33', to:'15:55.34'}
]

var fixturedFiles = new Map([
        [testFiles[0].name, testFiles[0]],
        [testFiles[1].name, testFiles[1]],
    ]
);

class FilesFixture extends  FilesInterface {

    listFiles() {
        let results = new Array();
        fixturedFiles.forEach((value, key, map) => {
            results.push(value)
        });
        $.Topic(Logscape.Explorer.Topics.setListFiles).publish(results);
    }

    fileContents(filename) {
        $.Topic(Logscape.Explorer.Topics.setFileContent).publish(
            fixturedFiles.get(filename).name + " made up file contents from the test fixture"
        );
    }
}

class RestVersion extends FilesInterface {

    listFiles() {
        // return files
        $.get(LOGSCAPE_URL + '/query/list', {},
            function(response) {
                $.Topic(Logscape.Explorer.Topics.setListFiles).publish(response);
            }
        )
    }

    fileContents(filename) {
        // return "this is the contents of a file from REST";
        $.Topic(Logscape.Explorer.Topics.setListFiles).publish(results);
    }

}

function binding () {
    // let filesFixture = new FilesFixture();
    let filesFixture = new RestVersion();

    console.log("Backend is using:" + filesFixture.constructor.name)

    // $.Topic(Logscape.Explorer.Topics.uploadFile).subscribe(function(event) {
    //     $.Topic(Logscape.Explorer.Topics.uploadFile).publish(filesFixture.upload());
    // })
    $.Topic(Logscape.Explorer.Topics.getListFiles).subscribe(function(event) {
        filesFixture.listFiles();
    })
    $.Topic(Logscape.Explorer.Topics.getFileContent).subscribe(function(event) {
        filesFixture.fileContents(event);
    })

}

