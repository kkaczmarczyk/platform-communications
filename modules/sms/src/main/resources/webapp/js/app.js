(function () {
    'use strict';

    /* App Module */

    angular.module('motech-sms', ['motech-dashboard', 'ngCookies', 'bootstrap', 'sendSmsService', 'settingsService']).config(['$routeProvider',
        function ($routeProvider) {
            $routeProvider.
                when('/send', {templateUrl: '../sms/resources/partials/sendSms.html', controller: 'SendSmsController'}).
                when('/settings', {templateUrl: '../sms/resources/partials/settings.html', controller: 'SettingsController'}).
                otherwise({redirectTo: '/send'});
    }]);
}());