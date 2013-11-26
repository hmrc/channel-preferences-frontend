describe("Common", function() {
    var fingerprint;
    beforeEach(function() {
        fingerprint = new Fingerprint( { screen_resolution: true } );
    });
    afterEach(function() {
        fingerprint = undefined;
    });
    describe("Test Device Fingerprint", function() {
      it("should create an instance of fingeprint object", function() {

        expect(fingerprint).not.toBeUndefined();
      });
      it("method get() was called", function() {
          spyOn(fingerprint, "get");
          fingerprint.get();
          expect(fingerprint.get).toHaveBeenCalled();
      });

    });
});