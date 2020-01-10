$(document).ready(function () {
    binding()
});

var files = [
    { name:'/this/is/a/file.txt', tag:'prod, uk', host:'someHost', size:'1024k', from:'10:00.21', to:'14:02.34'},
    { name:'/this/is/a/fileB.txt', tag:'prod, uk', host:'someHost', size:'1024k', from:'10:13.33', to:'15:55.34'}
]
class FilesInterface {
    listFiles() {
        return files
    }

    fileContents(filename) {
        throw new err ("not implemented")
    }

    set height(h) {
        throw new err ("not implemented")
    }

    get height() {
        throw new err ("not implemented")
    }
}
class FilesFixture extends  FilesInterface {
    listFiles() {
        return files
    }

    fileContents(filename) {
        return "this is the contents of a file from the fixture";
    }

    set height(h) {
        this._height = h;
    }

    get height() {
        return this._height;
    }
}


class RestVersion extends FilesInterface {
    listFiles() {
        return files
    }

    fileContents(filename) {
        return "this is the contents of a file from REST";
    }

    set height(h) {
        this._height = h;
    }

    get height() {
        return this._height;
    }
}

function binding () {
    console.log("Backend is now bound")
    // let filesFixture = new FilesFixture();
    let filesFixture = new RestVersion();
    $.Topic(Logscape.Explorer.Topics.getListFiles).subscribe(function(event) {
        $.Topic(Logscape.Explorer.Topics.setListFiles).publish(filesFixture.listFiles());
    })
    $.Topic(Logscape.Explorer.Topics.getFileContent).subscribe(function(event) {
        $.Topic(Logscape.Explorer.Topics.setFileContent).publish(filesFixture.fileContents(event));
    })

}

