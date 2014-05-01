'use strict';

angular.module('controllerUxApp', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute',
  'ui'
])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .when('/wizard/:id', {
        templateUrl: 'views/wizard.html',
        controller: 'wizard'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
