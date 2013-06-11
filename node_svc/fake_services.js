var http = require('http')

var respond = function(res, s) {
  res.writeHead(200);
  res.write(s);
  res.end();
}

var makeService = function(resources) {

  return http.createServer ( function(req, res) {
    console.log('Requested url is: ' + req.url);

    if (typeof(resources[req.url]) === 'undefined') {
      res.writeHead(404);
      res.end();
    }

    else {
      respond(res, resources[req.url]);
    }

  });

}


var AUTH_PORT = 6060;
var PERSONAL_PORT = 6061;


var authService = makeService( {'/auth/oid/jdensmore': '{"id":"/auth/oid/jdensmore","regimes":{"paye":"/personal/paye/AA020513B","sa":null,"vat":null,"ct":null},"createdAt":"1974-04-30T06:15:34.587Z"}'} );

authService.listen(AUTH_PORT);

console.log("Auth service running on port " + AUTH_PORT);


var personalService = makeService( {'/personal/paye/AA020513B': '{"nino":"AA020513B","firstName":"John","surname":"Densmore","name":"John Densmore","links":{"benefits":"/personal/paye/AA020513B/benefits/2013","taxCode":"/personal/paye/AA020513B/tax-codes/2013","employments":"/personal/paye/AA020513B/employments/2013"}}'});

personalService.listen(PERSONAL_PORT);

console.log("Personal service running on port " + PERSONAL_PORT);
