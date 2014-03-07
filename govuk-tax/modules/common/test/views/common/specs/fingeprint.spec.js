describe("Device Fingerprint", function() {
    var fingerprint;

    beforeEach(function() {
        fingerprint = new Mdtpdf();
    });
    afterEach(function() {
        fingerprint = undefined;
    });

    it("should return a device fingerprint object", function() {
        expect(fingerprint).not.toBeUndefined();
    });
    it("method get() was called", function() {

        spyOn(fingerprint, "get");
        fingerprint.get();
        expect(fingerprint.get).toHaveBeenCalled();
    });

    describe("returns", function() {
        var fpDetails;
        var originalNavigator = navigator;

        beforeAll(function() {
            fingerprint = new Mdtpdf( { screen_resolution: true } );

            var attributes = { userAgent: 'Mock user agent', language: 'Some language', platform: 'The os', cpuClass: 'The cpu class', doNotTrack: 'true', plugins: [{name: 'plugin name', description: 'plugin description'}]};
            $.each(attributes, function(key, value){
                navigator.__defineGetter__(key, function(){
                    return value;
                })
            })
        });

        afterAll(function() {
            fingerprint = undefined;
            navigator = originalNavigator;
            screen = originalScreen;
        });

        it("userAgent attribute", function() {
            fpDetails = $.parseJSON(fingerprint.get());
            expect(fpDetails.userAgent).toEqual('Mock user agent');
        });
        it("language attribute", function() {
            fpDetails = $.parseJSON(fingerprint.get());
            expect(fpDetails.language).toEqual('Some language');
        });
        it("platform attribute", function() {
            fpDetails = $.parseJSON(fingerprint.get());
            expect(fpDetails.platform).toEqual('The os');
        });
        it("cpuClass attribute", function() {
            fpDetails = $.parseJSON(fingerprint.get());
            expect(fpDetails.cpuClass).toEqual('The cpu class');
        });
        it("doNotTrack attribute", function() {
            fpDetails = $.parseJSON(fingerprint.get());
            expect(fpDetails.doNotTrack).toEqual(true);
        });
        it("number of plugins", function() {
            fpDetails = $.parseJSON(fingerprint.get());
            expect(fpDetails.numberOfPlugins).toEqual(1);
        });
        it("list of plugins", function() {
            spyOn(fingerprint, "getPluginsString");
            fpDetails = $.parseJSON(fingerprint.get());
            expect(fingerprint.getPluginsString).toHaveBeenCalled();
            expect(fpDetails.plugins).not.toBe(null);
        });

    });
});