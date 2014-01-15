module.exports = function(grunt) {

	grunt.initConfig({

		// JsHint your javascript
		jshint : {
			all : ['javascripts/*.js', '!javascripts/*.min.js', '!javascripts/*.min.js', '!javascripts/vendor/**/*.js'],
			options : {
				browser: true,
				curly: false,
				eqeqeq: false,
				eqnull: true,
				expr: true,
				immed: true,
				newcap: true,
				noarg: true,
				smarttabs: true,
				sub: true,
				undef: false
			}
		}
	});

	// Default task
	grunt.registerTask('default', ['jshint']);

	// Load up tasks
	grunt.loadNpmTasks('grunt-contrib-jshint');
};
