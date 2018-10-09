window.NFCPlugin = function()
{
    setTimeout(
            function () {
                cordova.exec(
                    function (data) {
                        alert(JSON.stringify(data));
                    },
                    function (reason) {
                        alert("Failed to initialize the NFCPlugin " + reason);
                    },
                    "NFCPlugin", "startNFC", []
                );
            }, 10
        );
}

//tag
