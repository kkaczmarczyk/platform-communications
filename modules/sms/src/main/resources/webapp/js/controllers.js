(function () {
    'use strict';

    /* Controllers */
    var smsModule = angular.module('motech-sms');

    smsModule.controller('SendSmsController', function ($scope, SendSmsService) {
        $scope.sms = {};

        $scope.sendSms = function () {
            SendSmsService.save(
                {},
                $scope.sms,
                function () {
                    motechAlert('sms.header.success', 'sms.sent');
                },
                function (response) {
                    handleWithStackTrace('sms.header.error', 'server.error', response);
                }
            );
        };
    });

    smsModule.controller('SettingsController', function ($scope, SettingsService) {
        $scope.settings = SettingsService.get();

        $scope.timeMultipliers = {
            'hours': $scope.msg('sms.settings.log.units.hours'),
            'days': $scope.msg('sms.settings.log.units.days'),
            'weeks': $scope.msg('sms.settings.log.units.weeks'),
            'months': $scope.msg('sms.settings.log.units.months'),
            'years': $scope.msg('sms.settings.log.units.years')
        };

        $scope.submit = function () {

            SettingsService.save(
                {},
                $scope.settings,
                function () {
                    motechAlert('sms.header.success', 'sms.settings.saved');
                    $scope.settings = SettingsService.get();
                },
                function (response) {
                    handleWithStackTrace('sms.header.error', 'server.error', response);
                }
            );
        };

        $scope.isNumeric = function (prop) {
            return $scope.settings.hasOwnProperty(prop) && /^[0-9]+$/.test($scope.settings[prop]);
        };

        $scope.purgeTimeControlsDisabled = function () {
            if ($scope.settings.logPurgeEnable.localeCompare("true") === 0) {
                return false;
            } else {
                return true;
            }
        };

    });
}());