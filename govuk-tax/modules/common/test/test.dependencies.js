// Dependencies for the unit test via Jasmine
EnvJasmine.loadGlobal(EnvJasmine.libDir + "jquery.js");
//EnvJasmine.loadGlobal(EnvJasmine.libDir + "angular.min.js");
//EnvJasmine.loadGlobal(EnvJasmine.libDir + "angular-resource.min.js");
//EnvJasmine.loadGlobal(EnvJasmine.testDir + "config/angular-mocks.js");

// Import all modules (in theory this should be done with RequireJs, but it fails for some reason)
// FIXME: check why we can't use RequireJs instead of manually importing files
//EnvJasmine.loadGlobal(EnvJasmine.rootDir + "todo.js");