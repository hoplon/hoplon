/**
 * @license root42 Inc. All Right Reserved.
 */
"use strict";

/**
 * @fileoverview Externs for moment.js 2.0.0
 * @externs
 */

/**
 * @interface
 * @author root42 Inc.
 */
function Moment() {};

/**
 * @nosideeffects
 * @type {(function():!Moment|function(!Date):!Moment|function(!number):!Moment|function(!Array.<!number>):!Moment|function(!string, !(string|Array.<!string>)=):!Moment|function(!Moment):!Moment)}
 */
function moment() {};

/**
 * @typedef {{seconds:?number, minutes:?number, hours:?number, weeks:?number, months:?number, years:?number}}
 */
Moment.DateRecord;

/**
 * @interface
 * @author root42 Inc.
 */
Moment.Duration = function() {};

/**
 * @since 1.6.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.unix = function() {};

/**
 * @since 1.5.0
 * @type {(function():!Moment|function(!number):!Moment|function(!Array.<!number>):!Moment|function(!string, !string=, !string=):!Moment|function(!string, !Array.<!string>):!Moment|function(!Moment):!Moment|function(!Date):!Moment)}
 */
Moment.prototype.utc = function() {};

/**
 * @nosideeffects
 * @since 1.7.0
 * @type {function():!boolean}
 */
Moment.prototype.isValid = function() {};

/**
 * @since 1.3.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.millisecond = function() {};

/**
 * @since 1.3.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.milliseconds = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.second = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.seconds = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.minute = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.minutes = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.hour = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.hours = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.date = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.dates = function() {};

/**
 * @since 1.3.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.day = function() {};

/**
 * @since 1.3.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.days = function() {};

/**
 * @since 2.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.dayOfYear = function() {};

/**
 * @since 2.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.week = function() {};

/**
 * @since 2.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.weeks = function() {};

/**
 * @since 2.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.isoWeek = function() {};

/**
 * @since 2.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.isoWeeks = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.month = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.months = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.year = function() {};

/**
 * @since 1.0.0
 * @type {(function():!number|function(!number):!Moment)}
 */
Moment.prototype.years = function() {};

/**
 * @since 1.0.0
 * @type {(function(!string, !number):!Moment|function(!number, !string):!Moment|function(!Moment.Duration):!Moment|function(!Moment.DateRecord):!Moment)}
 */
Moment.prototype.add = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {(function(!string, !number):!Moment|function(!number, !string):!Moment|function(!Moment.Duration):!Moment|function(!Moment.DateRecord):!Moment)}
 */
Moment.prototype.subtract = function() {};

/**
 * @nosideeffects
 * @since 1.7.0
 * @type {function(!string):!Moment}
 */
Moment.prototype.startOf = function() {};

/**
 * @nosideeffects
 * @since 1.7.0
 * @type {function(!string):!Moment}
 */
Moment.prototype.endOf = function() {};

/**
 * @nosideeffects
 * @since 1.5.0
 * @type {function():!Moment}
 */
Moment.prototype.local = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {(function():!Moment|function(!string):!Moment)}
 */
Moment.prototype.format = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {function(!boolean=):!Moment}
 */
Moment.prototype.fromNow = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {function(!(Moment|string|number|Date|Array.<!number>), !boolean=):!Moment}
 */
Moment.prototype.from = function() {};

/**
 * @nosideeffects
 * @since 1.3.0
 * @type {function():!Moment}
 */
Moment.prototype.calendar = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {function(!(Moment|string|number|Date|Array.<!number>), !string=, !boolean=):!number}
 */
Moment.prototype.diff = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {function():!number}
 */
Moment.prototype.valueOf = function() {};

/**
 * @nosideeffects
 * @since 1.2.0
 * @type {function():!Array.<!number>}
 */
Moment.prototype.zone = function() {};

/**
 * @nosideeffects
 * @since 1.5.0
 * @type {function():!number}
 */
Moment.prototype.daysInMonth = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {function():!Date}
 */
Moment.prototype.toDate = function() {};

/**
 * @nosideeffects
 * @since 2.0.0
 * @type {function():!Moment.DateRecord}
 */
Moment.prototype.toJSON = function() {};

/**
 * @nosideeffects
 * @since 2.0.0
 * @type {function(!(Moment|string|number|Date|Array.<!number>), !string=):!boolean}
 */
Moment.prototype.isBefore = function() {};

/**
 * @nosideeffects
 * @since 2.0.0
 * @type {function(!(Moment|string|number|Date|Array.<!number>), !string=):!boolean}
 */
Moment.prototype.isSame = function() {};

/**
 * @nosideeffects
 * @since 2.0.0
 * @type {function(!(Moment|string|number|Date|Array.<!number>), !string=):!boolean}
 */
Moment.prototype.isAfter = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {function():!boolean}
 */
Moment.prototype.isLearYear = function() {};

/**
 * @nosideeffects
 * @since 1.0.0
 * @type {function():!boolean}
 */
Moment.prototype.isDST = function() {};

/**
 * @nosideeffects
 * @since 1.5.0
 * @type {function(!Object):!boolean}
 */
Moment.prototype.isMoment = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {(function(!number, !string=):!Moment.Duration|function(!Moment.DateRecord):!Moment.Duration)}
 */
Moment.prototype.duration = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function(!boolean=):!string}
 */
Moment.Duration.prototype.humanize = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.milliseconds = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.asMilliseconds = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.seconds = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.asSeconds = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.minutes = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.asMinutes = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.hours = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.asHours = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.days = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.asDays = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.months = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.asMonths = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.years = function() {};

/**
 * @nosideeffects
 * @since 1.6.0
 * @type {function():!number}
 */
Moment.Duration.prototype.asYears = function() {};

/**
 * @nosideeffects
 * @since 2.1.0
 * @type {function():!number}
 */
Moment.Duration.prototype.isoWeekday = function() {};
