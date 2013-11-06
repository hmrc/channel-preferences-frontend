describe("Selecting Electric Car Type", function() {
var $form, $defaultOptions;
beforeEach(function() {
   loadFixtures("add_car_benefit_form.fixture.html");
    $form = $("#form-add-car-benefit");
    $defaultOptions = $form.find('*[data-default]');
});

it("should hide fuel type realted questions", function() {
     print("starting test");
    var spyEvent = spyOnEvent('#fuelType-electricity', 'click');

       spyOn(window, 'toggleDefaultOptions');
     $("#fuelType-electricity").trigger('click');
     //GOVUK.toggleDefaultOptions();
    // $form.addClass("someclass");

     expect( 'click' ).toHaveBeenTriggeredOn( '#fuelType-electricity' );
     expect( spyEvent ).toHaveBeenTriggered();
     //expect($("#fuelType-electricity")).toExist();
     //console.log($(".visuallyhidden").length)//.toHaveLength(1)
     //expect(window.toggleDefaultOptions).toHaveBeenCalled();
     /*waitsFor(function() {
          //expect($(".visuallyhidden")).toHaveLength(3);
          console.log($(".visuallyhidden").length);
     }, "error", 200);  */


});
});