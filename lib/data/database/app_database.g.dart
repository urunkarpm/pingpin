// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'app_database.dart';

// ignore_for_file: type=lint
class $OfficeConfigsTable extends OfficeConfigs
    with TableInfo<$OfficeConfigsTable, OfficeConfig> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $OfficeConfigsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
      'id', aliasedName, false,
      hasAutoIncrement: true,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultConstraints:
          GeneratedColumn.constraintIsAlways('PRIMARY KEY AUTOINCREMENT'));
  static const VerificationMeta _ssidMeta = const VerificationMeta('ssid');
  @override
  late final GeneratedColumn<String> ssid = GeneratedColumn<String>(
      'ssid', aliasedName, false,
      additionalChecks:
          GeneratedColumn.checkTextLength(minTextLength: 1, maxTextLength: 255),
      type: DriftSqlType.string,
      requiredDuringInsert: true);
  static const VerificationMeta _latitudeMeta =
      const VerificationMeta('latitude');
  @override
  late final GeneratedColumn<double> latitude = GeneratedColumn<double>(
      'latitude', aliasedName, false,
      type: DriftSqlType.double, requiredDuringInsert: true);
  static const VerificationMeta _longitudeMeta =
      const VerificationMeta('longitude');
  @override
  late final GeneratedColumn<double> longitude = GeneratedColumn<double>(
      'longitude', aliasedName, false,
      type: DriftSqlType.double, requiredDuringInsert: true);
  static const VerificationMeta _radiusMetersMeta =
      const VerificationMeta('radiusMeters');
  @override
  late final GeneratedColumn<int> radiusMeters = GeneratedColumn<int>(
      'radius_meters', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(100));
  static const VerificationMeta _lateCutoffTimeMeta =
      const VerificationMeta('lateCutoffTime');
  @override
  late final GeneratedColumn<String> lateCutoffTime = GeneratedColumn<String>(
      'late_cutoff_time', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('10:30'));
  static const VerificationMeta _checkInTimeMeta =
      const VerificationMeta('checkInTime');
  @override
  late final GeneratedColumn<String> checkInTime = GeneratedColumn<String>(
      'check_in_time', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('09:30'));
  static const VerificationMeta _checkOutTimeMeta =
      const VerificationMeta('checkOutTime');
  @override
  late final GeneratedColumn<String> checkOutTime = GeneratedColumn<String>(
      'check_out_time', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant('17:30'));
  static const VerificationMeta _portalUrlMeta =
      const VerificationMeta('portalUrl');
  @override
  late final GeneratedColumn<String> portalUrl = GeneratedColumn<String>(
      'portal_url', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: false,
      defaultValue: const Constant(''));
  static const VerificationMeta _workingDaysMaskMeta =
      const VerificationMeta('workingDaysMask');
  @override
  late final GeneratedColumn<int> workingDaysMask = GeneratedColumn<int>(
      'working_days_mask', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(31));
  static const VerificationMeta _wfoDaysMaskMeta =
      const VerificationMeta('wfoDaysMask');
  @override
  late final GeneratedColumn<int> wfoDaysMask = GeneratedColumn<int>(
      'wfo_days_mask', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(31));
  static const VerificationMeta _createdAtMeta =
      const VerificationMeta('createdAt');
  @override
  late final GeneratedColumn<DateTime> createdAt = GeneratedColumn<DateTime>(
      'created_at', aliasedName, false,
      type: DriftSqlType.dateTime,
      requiredDuringInsert: false,
      defaultValue: currentDateAndTime);
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<DateTime> updatedAt = GeneratedColumn<DateTime>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.dateTime,
      requiredDuringInsert: false,
      defaultValue: currentDateAndTime);
  @override
  List<GeneratedColumn> get $columns => [
        id,
        ssid,
        latitude,
        longitude,
        radiusMeters,
        lateCutoffTime,
        checkInTime,
        checkOutTime,
        portalUrl,
        workingDaysMask,
        wfoDaysMask,
        createdAt,
        updatedAt
      ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'office_configs';
  @override
  VerificationContext validateIntegrity(Insertable<OfficeConfig> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('ssid')) {
      context.handle(
          _ssidMeta, ssid.isAcceptableOrUnknown(data['ssid']!, _ssidMeta));
    } else if (isInserting) {
      context.missing(_ssidMeta);
    }
    if (data.containsKey('latitude')) {
      context.handle(_latitudeMeta,
          latitude.isAcceptableOrUnknown(data['latitude']!, _latitudeMeta));
    } else if (isInserting) {
      context.missing(_latitudeMeta);
    }
    if (data.containsKey('longitude')) {
      context.handle(_longitudeMeta,
          longitude.isAcceptableOrUnknown(data['longitude']!, _longitudeMeta));
    } else if (isInserting) {
      context.missing(_longitudeMeta);
    }
    if (data.containsKey('radius_meters')) {
      context.handle(
          _radiusMetersMeta,
          radiusMeters.isAcceptableOrUnknown(
              data['radius_meters']!, _radiusMetersMeta));
    }
    if (data.containsKey('late_cutoff_time')) {
      context.handle(
          _lateCutoffTimeMeta,
          lateCutoffTime.isAcceptableOrUnknown(
              data['late_cutoff_time']!, _lateCutoffTimeMeta));
    }
    if (data.containsKey('check_in_time')) {
      context.handle(
          _checkInTimeMeta,
          checkInTime.isAcceptableOrUnknown(
              data['check_in_time']!, _checkInTimeMeta));
    }
    if (data.containsKey('check_out_time')) {
      context.handle(
          _checkOutTimeMeta,
          checkOutTime.isAcceptableOrUnknown(
              data['check_out_time']!, _checkOutTimeMeta));
    }
    if (data.containsKey('portal_url')) {
      context.handle(_portalUrlMeta,
          portalUrl.isAcceptableOrUnknown(data['portal_url']!, _portalUrlMeta));
    }
    if (data.containsKey('working_days_mask')) {
      context.handle(
          _workingDaysMaskMeta,
          workingDaysMask.isAcceptableOrUnknown(
              data['working_days_mask']!, _workingDaysMaskMeta));
    }
    if (data.containsKey('wfo_days_mask')) {
      context.handle(
          _wfoDaysMaskMeta,
          wfoDaysMask.isAcceptableOrUnknown(
              data['wfo_days_mask']!, _wfoDaysMaskMeta));
    }
    if (data.containsKey('created_at')) {
      context.handle(_createdAtMeta,
          createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  OfficeConfig map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return OfficeConfig(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}id'])!,
      ssid: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}ssid'])!,
      latitude: attachedDatabase.typeMapping
          .read(DriftSqlType.double, data['${effectivePrefix}latitude'])!,
      longitude: attachedDatabase.typeMapping
          .read(DriftSqlType.double, data['${effectivePrefix}longitude'])!,
      radiusMeters: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}radius_meters'])!,
      lateCutoffTime: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}late_cutoff_time'])!,
      checkInTime: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}check_in_time'])!,
      checkOutTime: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}check_out_time'])!,
      portalUrl: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}portal_url'])!,
      workingDaysMask: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}working_days_mask'])!,
      wfoDaysMask: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}wfo_days_mask'])!,
      createdAt: attachedDatabase.typeMapping
          .read(DriftSqlType.dateTime, data['${effectivePrefix}created_at'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.dateTime, data['${effectivePrefix}updated_at'])!,
    );
  }

  @override
  $OfficeConfigsTable createAlias(String alias) {
    return $OfficeConfigsTable(attachedDatabase, alias);
  }
}

class OfficeConfig extends DataClass implements Insertable<OfficeConfig> {
  final int id;
  final String ssid;
  final double latitude;
  final double longitude;
  final int radiusMeters;
  final String lateCutoffTime;
  final String checkInTime;
  final String checkOutTime;
  final String portalUrl;
  final int workingDaysMask;
  final int wfoDaysMask;
  final DateTime createdAt;
  final DateTime updatedAt;
  const OfficeConfig(
      {required this.id,
      required this.ssid,
      required this.latitude,
      required this.longitude,
      required this.radiusMeters,
      required this.lateCutoffTime,
      required this.checkInTime,
      required this.checkOutTime,
      required this.portalUrl,
      required this.workingDaysMask,
      required this.wfoDaysMask,
      required this.createdAt,
      required this.updatedAt});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['ssid'] = Variable<String>(ssid);
    map['latitude'] = Variable<double>(latitude);
    map['longitude'] = Variable<double>(longitude);
    map['radius_meters'] = Variable<int>(radiusMeters);
    map['late_cutoff_time'] = Variable<String>(lateCutoffTime);
    map['check_in_time'] = Variable<String>(checkInTime);
    map['check_out_time'] = Variable<String>(checkOutTime);
    map['portal_url'] = Variable<String>(portalUrl);
    map['working_days_mask'] = Variable<int>(workingDaysMask);
    map['wfo_days_mask'] = Variable<int>(wfoDaysMask);
    map['created_at'] = Variable<DateTime>(createdAt);
    map['updated_at'] = Variable<DateTime>(updatedAt);
    return map;
  }

  OfficeConfigsCompanion toCompanion(bool nullToAbsent) {
    return OfficeConfigsCompanion(
      id: Value(id),
      ssid: Value(ssid),
      latitude: Value(latitude),
      longitude: Value(longitude),
      radiusMeters: Value(radiusMeters),
      lateCutoffTime: Value(lateCutoffTime),
      checkInTime: Value(checkInTime),
      checkOutTime: Value(checkOutTime),
      portalUrl: Value(portalUrl),
      workingDaysMask: Value(workingDaysMask),
      wfoDaysMask: Value(wfoDaysMask),
      createdAt: Value(createdAt),
      updatedAt: Value(updatedAt),
    );
  }

  factory OfficeConfig.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return OfficeConfig(
      id: serializer.fromJson<int>(json['id']),
      ssid: serializer.fromJson<String>(json['ssid']),
      latitude: serializer.fromJson<double>(json['latitude']),
      longitude: serializer.fromJson<double>(json['longitude']),
      radiusMeters: serializer.fromJson<int>(json['radiusMeters']),
      lateCutoffTime: serializer.fromJson<String>(json['lateCutoffTime']),
      checkInTime: serializer.fromJson<String>(json['checkInTime']),
      checkOutTime: serializer.fromJson<String>(json['checkOutTime']),
      portalUrl: serializer.fromJson<String>(json['portalUrl']),
      workingDaysMask: serializer.fromJson<int>(json['workingDaysMask']),
      wfoDaysMask: serializer.fromJson<int>(json['wfoDaysMask']),
      createdAt: serializer.fromJson<DateTime>(json['createdAt']),
      updatedAt: serializer.fromJson<DateTime>(json['updatedAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'ssid': serializer.toJson<String>(ssid),
      'latitude': serializer.toJson<double>(latitude),
      'longitude': serializer.toJson<double>(longitude),
      'radiusMeters': serializer.toJson<int>(radiusMeters),
      'lateCutoffTime': serializer.toJson<String>(lateCutoffTime),
      'checkInTime': serializer.toJson<String>(checkInTime),
      'checkOutTime': serializer.toJson<String>(checkOutTime),
      'portalUrl': serializer.toJson<String>(portalUrl),
      'workingDaysMask': serializer.toJson<int>(workingDaysMask),
      'wfoDaysMask': serializer.toJson<int>(wfoDaysMask),
      'createdAt': serializer.toJson<DateTime>(createdAt),
      'updatedAt': serializer.toJson<DateTime>(updatedAt),
    };
  }

  OfficeConfig copyWith(
          {int? id,
          String? ssid,
          double? latitude,
          double? longitude,
          int? radiusMeters,
          String? lateCutoffTime,
          String? checkInTime,
          String? checkOutTime,
          String? portalUrl,
          int? workingDaysMask,
          int? wfoDaysMask,
          DateTime? createdAt,
          DateTime? updatedAt}) =>
      OfficeConfig(
        id: id ?? this.id,
        ssid: ssid ?? this.ssid,
        latitude: latitude ?? this.latitude,
        longitude: longitude ?? this.longitude,
        radiusMeters: radiusMeters ?? this.radiusMeters,
        lateCutoffTime: lateCutoffTime ?? this.lateCutoffTime,
        checkInTime: checkInTime ?? this.checkInTime,
        checkOutTime: checkOutTime ?? this.checkOutTime,
        portalUrl: portalUrl ?? this.portalUrl,
        workingDaysMask: workingDaysMask ?? this.workingDaysMask,
        wfoDaysMask: wfoDaysMask ?? this.wfoDaysMask,
        createdAt: createdAt ?? this.createdAt,
        updatedAt: updatedAt ?? this.updatedAt,
      );
  OfficeConfig copyWithCompanion(OfficeConfigsCompanion data) {
    return OfficeConfig(
      id: data.id.present ? data.id.value : this.id,
      ssid: data.ssid.present ? data.ssid.value : this.ssid,
      latitude: data.latitude.present ? data.latitude.value : this.latitude,
      longitude: data.longitude.present ? data.longitude.value : this.longitude,
      radiusMeters: data.radiusMeters.present
          ? data.radiusMeters.value
          : this.radiusMeters,
      lateCutoffTime: data.lateCutoffTime.present
          ? data.lateCutoffTime.value
          : this.lateCutoffTime,
      checkInTime:
          data.checkInTime.present ? data.checkInTime.value : this.checkInTime,
      checkOutTime: data.checkOutTime.present
          ? data.checkOutTime.value
          : this.checkOutTime,
      portalUrl: data.portalUrl.present ? data.portalUrl.value : this.portalUrl,
      workingDaysMask: data.workingDaysMask.present
          ? data.workingDaysMask.value
          : this.workingDaysMask,
      wfoDaysMask:
          data.wfoDaysMask.present ? data.wfoDaysMask.value : this.wfoDaysMask,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('OfficeConfig(')
          ..write('id: $id, ')
          ..write('ssid: $ssid, ')
          ..write('latitude: $latitude, ')
          ..write('longitude: $longitude, ')
          ..write('radiusMeters: $radiusMeters, ')
          ..write('lateCutoffTime: $lateCutoffTime, ')
          ..write('checkInTime: $checkInTime, ')
          ..write('checkOutTime: $checkOutTime, ')
          ..write('portalUrl: $portalUrl, ')
          ..write('workingDaysMask: $workingDaysMask, ')
          ..write('wfoDaysMask: $wfoDaysMask, ')
          ..write('createdAt: $createdAt, ')
          ..write('updatedAt: $updatedAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id,
      ssid,
      latitude,
      longitude,
      radiusMeters,
      lateCutoffTime,
      checkInTime,
      checkOutTime,
      portalUrl,
      workingDaysMask,
      wfoDaysMask,
      createdAt,
      updatedAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is OfficeConfig &&
          other.id == this.id &&
          other.ssid == this.ssid &&
          other.latitude == this.latitude &&
          other.longitude == this.longitude &&
          other.radiusMeters == this.radiusMeters &&
          other.lateCutoffTime == this.lateCutoffTime &&
          other.checkInTime == this.checkInTime &&
          other.checkOutTime == this.checkOutTime &&
          other.portalUrl == this.portalUrl &&
          other.workingDaysMask == this.workingDaysMask &&
          other.wfoDaysMask == this.wfoDaysMask &&
          other.createdAt == this.createdAt &&
          other.updatedAt == this.updatedAt);
}

class OfficeConfigsCompanion extends UpdateCompanion<OfficeConfig> {
  final Value<int> id;
  final Value<String> ssid;
  final Value<double> latitude;
  final Value<double> longitude;
  final Value<int> radiusMeters;
  final Value<String> lateCutoffTime;
  final Value<String> checkInTime;
  final Value<String> checkOutTime;
  final Value<String> portalUrl;
  final Value<int> workingDaysMask;
  final Value<int> wfoDaysMask;
  final Value<DateTime> createdAt;
  final Value<DateTime> updatedAt;
  const OfficeConfigsCompanion({
    this.id = const Value.absent(),
    this.ssid = const Value.absent(),
    this.latitude = const Value.absent(),
    this.longitude = const Value.absent(),
    this.radiusMeters = const Value.absent(),
    this.lateCutoffTime = const Value.absent(),
    this.checkInTime = const Value.absent(),
    this.checkOutTime = const Value.absent(),
    this.portalUrl = const Value.absent(),
    this.workingDaysMask = const Value.absent(),
    this.wfoDaysMask = const Value.absent(),
    this.createdAt = const Value.absent(),
    this.updatedAt = const Value.absent(),
  });
  OfficeConfigsCompanion.insert({
    this.id = const Value.absent(),
    required String ssid,
    required double latitude,
    required double longitude,
    this.radiusMeters = const Value.absent(),
    this.lateCutoffTime = const Value.absent(),
    this.checkInTime = const Value.absent(),
    this.checkOutTime = const Value.absent(),
    this.portalUrl = const Value.absent(),
    this.workingDaysMask = const Value.absent(),
    this.wfoDaysMask = const Value.absent(),
    this.createdAt = const Value.absent(),
    this.updatedAt = const Value.absent(),
  })  : ssid = Value(ssid),
        latitude = Value(latitude),
        longitude = Value(longitude);
  static Insertable<OfficeConfig> custom({
    Expression<int>? id,
    Expression<String>? ssid,
    Expression<double>? latitude,
    Expression<double>? longitude,
    Expression<int>? radiusMeters,
    Expression<String>? lateCutoffTime,
    Expression<String>? checkInTime,
    Expression<String>? checkOutTime,
    Expression<String>? portalUrl,
    Expression<int>? workingDaysMask,
    Expression<int>? wfoDaysMask,
    Expression<DateTime>? createdAt,
    Expression<DateTime>? updatedAt,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (ssid != null) 'ssid': ssid,
      if (latitude != null) 'latitude': latitude,
      if (longitude != null) 'longitude': longitude,
      if (radiusMeters != null) 'radius_meters': radiusMeters,
      if (lateCutoffTime != null) 'late_cutoff_time': lateCutoffTime,
      if (checkInTime != null) 'check_in_time': checkInTime,
      if (checkOutTime != null) 'check_out_time': checkOutTime,
      if (portalUrl != null) 'portal_url': portalUrl,
      if (workingDaysMask != null) 'working_days_mask': workingDaysMask,
      if (wfoDaysMask != null) 'wfo_days_mask': wfoDaysMask,
      if (createdAt != null) 'created_at': createdAt,
      if (updatedAt != null) 'updated_at': updatedAt,
    });
  }

  OfficeConfigsCompanion copyWith(
      {Value<int>? id,
      Value<String>? ssid,
      Value<double>? latitude,
      Value<double>? longitude,
      Value<int>? radiusMeters,
      Value<String>? lateCutoffTime,
      Value<String>? checkInTime,
      Value<String>? checkOutTime,
      Value<String>? portalUrl,
      Value<int>? workingDaysMask,
      Value<int>? wfoDaysMask,
      Value<DateTime>? createdAt,
      Value<DateTime>? updatedAt}) {
    return OfficeConfigsCompanion(
      id: id ?? this.id,
      ssid: ssid ?? this.ssid,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      radiusMeters: radiusMeters ?? this.radiusMeters,
      lateCutoffTime: lateCutoffTime ?? this.lateCutoffTime,
      checkInTime: checkInTime ?? this.checkInTime,
      checkOutTime: checkOutTime ?? this.checkOutTime,
      portalUrl: portalUrl ?? this.portalUrl,
      workingDaysMask: workingDaysMask ?? this.workingDaysMask,
      wfoDaysMask: wfoDaysMask ?? this.wfoDaysMask,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (ssid.present) {
      map['ssid'] = Variable<String>(ssid.value);
    }
    if (latitude.present) {
      map['latitude'] = Variable<double>(latitude.value);
    }
    if (longitude.present) {
      map['longitude'] = Variable<double>(longitude.value);
    }
    if (radiusMeters.present) {
      map['radius_meters'] = Variable<int>(radiusMeters.value);
    }
    if (lateCutoffTime.present) {
      map['late_cutoff_time'] = Variable<String>(lateCutoffTime.value);
    }
    if (checkInTime.present) {
      map['check_in_time'] = Variable<String>(checkInTime.value);
    }
    if (checkOutTime.present) {
      map['check_out_time'] = Variable<String>(checkOutTime.value);
    }
    if (portalUrl.present) {
      map['portal_url'] = Variable<String>(portalUrl.value);
    }
    if (workingDaysMask.present) {
      map['working_days_mask'] = Variable<int>(workingDaysMask.value);
    }
    if (wfoDaysMask.present) {
      map['wfo_days_mask'] = Variable<int>(wfoDaysMask.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<DateTime>(createdAt.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<DateTime>(updatedAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('OfficeConfigsCompanion(')
          ..write('id: $id, ')
          ..write('ssid: $ssid, ')
          ..write('latitude: $latitude, ')
          ..write('longitude: $longitude, ')
          ..write('radiusMeters: $radiusMeters, ')
          ..write('lateCutoffTime: $lateCutoffTime, ')
          ..write('checkInTime: $checkInTime, ')
          ..write('checkOutTime: $checkOutTime, ')
          ..write('portalUrl: $portalUrl, ')
          ..write('workingDaysMask: $workingDaysMask, ')
          ..write('wfoDaysMask: $wfoDaysMask, ')
          ..write('createdAt: $createdAt, ')
          ..write('updatedAt: $updatedAt')
          ..write(')'))
        .toString();
  }
}

class $AttendanceRecordsTable extends AttendanceRecords
    with TableInfo<$AttendanceRecordsTable, AttendanceRecord> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $AttendanceRecordsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
      'id', aliasedName, false,
      hasAutoIncrement: true,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultConstraints:
          GeneratedColumn.constraintIsAlways('PRIMARY KEY AUTOINCREMENT'));
  static const VerificationMeta _dateYyyyMmDdMeta =
      const VerificationMeta('dateYyyyMmDd');
  @override
  late final GeneratedColumn<String> dateYyyyMmDd = GeneratedColumn<String>(
      'date_yyyy_mm_dd', aliasedName, false,
      type: DriftSqlType.string,
      requiredDuringInsert: true,
      defaultConstraints: GeneratedColumn.constraintIsAlways('UNIQUE'));
  @override
  late final GeneratedColumnWithTypeConverter<AttendanceStatus, String> status =
      GeneratedColumn<String>('status', aliasedName, false,
              type: DriftSqlType.string, requiredDuringInsert: true)
          .withConverter<AttendanceStatus>(
              $AttendanceRecordsTable.$converterstatus);
  static const VerificationMeta _markedAtMeta =
      const VerificationMeta('markedAt');
  @override
  late final GeneratedColumn<DateTime> markedAt = GeneratedColumn<DateTime>(
      'marked_at', aliasedName, false,
      type: DriftSqlType.dateTime, requiredDuringInsert: true);
  static const VerificationMeta _ssidSnapshotMeta =
      const VerificationMeta('ssidSnapshot');
  @override
  late final GeneratedColumn<String> ssidSnapshot = GeneratedColumn<String>(
      'ssid_snapshot', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _distanceMetersMeta =
      const VerificationMeta('distanceMeters');
  @override
  late final GeneratedColumn<double> distanceMeters = GeneratedColumn<double>(
      'distance_meters', aliasedName, true,
      type: DriftSqlType.double, requiredDuringInsert: false);
  @override
  List<GeneratedColumn> get $columns =>
      [id, dateYyyyMmDd, status, markedAt, ssidSnapshot, distanceMeters];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'attendance_records';
  @override
  VerificationContext validateIntegrity(Insertable<AttendanceRecord> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('date_yyyy_mm_dd')) {
      context.handle(
          _dateYyyyMmDdMeta,
          dateYyyyMmDd.isAcceptableOrUnknown(
              data['date_yyyy_mm_dd']!, _dateYyyyMmDdMeta));
    } else if (isInserting) {
      context.missing(_dateYyyyMmDdMeta);
    }
    if (data.containsKey('marked_at')) {
      context.handle(_markedAtMeta,
          markedAt.isAcceptableOrUnknown(data['marked_at']!, _markedAtMeta));
    } else if (isInserting) {
      context.missing(_markedAtMeta);
    }
    if (data.containsKey('ssid_snapshot')) {
      context.handle(
          _ssidSnapshotMeta,
          ssidSnapshot.isAcceptableOrUnknown(
              data['ssid_snapshot']!, _ssidSnapshotMeta));
    }
    if (data.containsKey('distance_meters')) {
      context.handle(
          _distanceMetersMeta,
          distanceMeters.isAcceptableOrUnknown(
              data['distance_meters']!, _distanceMetersMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  AttendanceRecord map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return AttendanceRecord(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}id'])!,
      dateYyyyMmDd: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}date_yyyy_mm_dd'])!,
      status: $AttendanceRecordsTable.$converterstatus.fromSql(attachedDatabase
          .typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}status'])!),
      markedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.dateTime, data['${effectivePrefix}marked_at'])!,
      ssidSnapshot: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}ssid_snapshot']),
      distanceMeters: attachedDatabase.typeMapping
          .read(DriftSqlType.double, data['${effectivePrefix}distance_meters']),
    );
  }

  @override
  $AttendanceRecordsTable createAlias(String alias) {
    return $AttendanceRecordsTable(attachedDatabase, alias);
  }

  static TypeConverter<AttendanceStatus, String> $converterstatus =
      AttendanceStatusConverter();
}

class AttendanceRecord extends DataClass
    implements Insertable<AttendanceRecord> {
  final int id;
  final String dateYyyyMmDd;
  final AttendanceStatus status;
  final DateTime markedAt;
  final String? ssidSnapshot;
  final double? distanceMeters;
  const AttendanceRecord(
      {required this.id,
      required this.dateYyyyMmDd,
      required this.status,
      required this.markedAt,
      this.ssidSnapshot,
      this.distanceMeters});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['date_yyyy_mm_dd'] = Variable<String>(dateYyyyMmDd);
    {
      map['status'] = Variable<String>(
          $AttendanceRecordsTable.$converterstatus.toSql(status));
    }
    map['marked_at'] = Variable<DateTime>(markedAt);
    if (!nullToAbsent || ssidSnapshot != null) {
      map['ssid_snapshot'] = Variable<String>(ssidSnapshot);
    }
    if (!nullToAbsent || distanceMeters != null) {
      map['distance_meters'] = Variable<double>(distanceMeters);
    }
    return map;
  }

  AttendanceRecordsCompanion toCompanion(bool nullToAbsent) {
    return AttendanceRecordsCompanion(
      id: Value(id),
      dateYyyyMmDd: Value(dateYyyyMmDd),
      status: Value(status),
      markedAt: Value(markedAt),
      ssidSnapshot: ssidSnapshot == null && nullToAbsent
          ? const Value.absent()
          : Value(ssidSnapshot),
      distanceMeters: distanceMeters == null && nullToAbsent
          ? const Value.absent()
          : Value(distanceMeters),
    );
  }

  factory AttendanceRecord.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return AttendanceRecord(
      id: serializer.fromJson<int>(json['id']),
      dateYyyyMmDd: serializer.fromJson<String>(json['dateYyyyMmDd']),
      status: serializer.fromJson<AttendanceStatus>(json['status']),
      markedAt: serializer.fromJson<DateTime>(json['markedAt']),
      ssidSnapshot: serializer.fromJson<String?>(json['ssidSnapshot']),
      distanceMeters: serializer.fromJson<double?>(json['distanceMeters']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'dateYyyyMmDd': serializer.toJson<String>(dateYyyyMmDd),
      'status': serializer.toJson<AttendanceStatus>(status),
      'markedAt': serializer.toJson<DateTime>(markedAt),
      'ssidSnapshot': serializer.toJson<String?>(ssidSnapshot),
      'distanceMeters': serializer.toJson<double?>(distanceMeters),
    };
  }

  AttendanceRecord copyWith(
          {int? id,
          String? dateYyyyMmDd,
          AttendanceStatus? status,
          DateTime? markedAt,
          Value<String?> ssidSnapshot = const Value.absent(),
          Value<double?> distanceMeters = const Value.absent()}) =>
      AttendanceRecord(
        id: id ?? this.id,
        dateYyyyMmDd: dateYyyyMmDd ?? this.dateYyyyMmDd,
        status: status ?? this.status,
        markedAt: markedAt ?? this.markedAt,
        ssidSnapshot:
            ssidSnapshot.present ? ssidSnapshot.value : this.ssidSnapshot,
        distanceMeters:
            distanceMeters.present ? distanceMeters.value : this.distanceMeters,
      );
  AttendanceRecord copyWithCompanion(AttendanceRecordsCompanion data) {
    return AttendanceRecord(
      id: data.id.present ? data.id.value : this.id,
      dateYyyyMmDd: data.dateYyyyMmDd.present
          ? data.dateYyyyMmDd.value
          : this.dateYyyyMmDd,
      status: data.status.present ? data.status.value : this.status,
      markedAt: data.markedAt.present ? data.markedAt.value : this.markedAt,
      ssidSnapshot: data.ssidSnapshot.present
          ? data.ssidSnapshot.value
          : this.ssidSnapshot,
      distanceMeters: data.distanceMeters.present
          ? data.distanceMeters.value
          : this.distanceMeters,
    );
  }

  @override
  String toString() {
    return (StringBuffer('AttendanceRecord(')
          ..write('id: $id, ')
          ..write('dateYyyyMmDd: $dateYyyyMmDd, ')
          ..write('status: $status, ')
          ..write('markedAt: $markedAt, ')
          ..write('ssidSnapshot: $ssidSnapshot, ')
          ..write('distanceMeters: $distanceMeters')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id, dateYyyyMmDd, status, markedAt, ssidSnapshot, distanceMeters);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is AttendanceRecord &&
          other.id == this.id &&
          other.dateYyyyMmDd == this.dateYyyyMmDd &&
          other.status == this.status &&
          other.markedAt == this.markedAt &&
          other.ssidSnapshot == this.ssidSnapshot &&
          other.distanceMeters == this.distanceMeters);
}

class AttendanceRecordsCompanion extends UpdateCompanion<AttendanceRecord> {
  final Value<int> id;
  final Value<String> dateYyyyMmDd;
  final Value<AttendanceStatus> status;
  final Value<DateTime> markedAt;
  final Value<String?> ssidSnapshot;
  final Value<double?> distanceMeters;
  const AttendanceRecordsCompanion({
    this.id = const Value.absent(),
    this.dateYyyyMmDd = const Value.absent(),
    this.status = const Value.absent(),
    this.markedAt = const Value.absent(),
    this.ssidSnapshot = const Value.absent(),
    this.distanceMeters = const Value.absent(),
  });
  AttendanceRecordsCompanion.insert({
    this.id = const Value.absent(),
    required String dateYyyyMmDd,
    required AttendanceStatus status,
    required DateTime markedAt,
    this.ssidSnapshot = const Value.absent(),
    this.distanceMeters = const Value.absent(),
  })  : dateYyyyMmDd = Value(dateYyyyMmDd),
        status = Value(status),
        markedAt = Value(markedAt);
  static Insertable<AttendanceRecord> custom({
    Expression<int>? id,
    Expression<String>? dateYyyyMmDd,
    Expression<String>? status,
    Expression<DateTime>? markedAt,
    Expression<String>? ssidSnapshot,
    Expression<double>? distanceMeters,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (dateYyyyMmDd != null) 'date_yyyy_mm_dd': dateYyyyMmDd,
      if (status != null) 'status': status,
      if (markedAt != null) 'marked_at': markedAt,
      if (ssidSnapshot != null) 'ssid_snapshot': ssidSnapshot,
      if (distanceMeters != null) 'distance_meters': distanceMeters,
    });
  }

  AttendanceRecordsCompanion copyWith(
      {Value<int>? id,
      Value<String>? dateYyyyMmDd,
      Value<AttendanceStatus>? status,
      Value<DateTime>? markedAt,
      Value<String?>? ssidSnapshot,
      Value<double?>? distanceMeters}) {
    return AttendanceRecordsCompanion(
      id: id ?? this.id,
      dateYyyyMmDd: dateYyyyMmDd ?? this.dateYyyyMmDd,
      status: status ?? this.status,
      markedAt: markedAt ?? this.markedAt,
      ssidSnapshot: ssidSnapshot ?? this.ssidSnapshot,
      distanceMeters: distanceMeters ?? this.distanceMeters,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (dateYyyyMmDd.present) {
      map['date_yyyy_mm_dd'] = Variable<String>(dateYyyyMmDd.value);
    }
    if (status.present) {
      map['status'] = Variable<String>(
          $AttendanceRecordsTable.$converterstatus.toSql(status.value));
    }
    if (markedAt.present) {
      map['marked_at'] = Variable<DateTime>(markedAt.value);
    }
    if (ssidSnapshot.present) {
      map['ssid_snapshot'] = Variable<String>(ssidSnapshot.value);
    }
    if (distanceMeters.present) {
      map['distance_meters'] = Variable<double>(distanceMeters.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('AttendanceRecordsCompanion(')
          ..write('id: $id, ')
          ..write('dateYyyyMmDd: $dateYyyyMmDd, ')
          ..write('status: $status, ')
          ..write('markedAt: $markedAt, ')
          ..write('ssidSnapshot: $ssidSnapshot, ')
          ..write('distanceMeters: $distanceMeters')
          ..write(')'))
        .toString();
  }
}

class $UserProfilesTable extends UserProfiles
    with TableInfo<$UserProfilesTable, UserProfile> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $UserProfilesTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
      'id', aliasedName, false,
      hasAutoIncrement: true,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultConstraints:
          GeneratedColumn.constraintIsAlways('PRIMARY KEY AUTOINCREMENT'));
  static const VerificationMeta _fullNameMeta =
      const VerificationMeta('fullName');
  @override
  late final GeneratedColumn<String> fullName = GeneratedColumn<String>(
      'full_name', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _designationMeta =
      const VerificationMeta('designation');
  @override
  late final GeneratedColumn<String> designation = GeneratedColumn<String>(
      'designation', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _photoPathMeta =
      const VerificationMeta('photoPath');
  @override
  late final GeneratedColumn<String> photoPath = GeneratedColumn<String>(
      'photo_path', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _employeeIdMeta =
      const VerificationMeta('employeeId');
  @override
  late final GeneratedColumn<String> employeeId = GeneratedColumn<String>(
      'employee_id', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _emailMeta = const VerificationMeta('email');
  @override
  late final GeneratedColumn<String> email = GeneratedColumn<String>(
      'email', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _phoneMeta = const VerificationMeta('phone');
  @override
  late final GeneratedColumn<String> phone = GeneratedColumn<String>(
      'phone', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<DateTime> updatedAt = GeneratedColumn<DateTime>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.dateTime,
      requiredDuringInsert: false,
      defaultValue: currentDateAndTime);
  @override
  List<GeneratedColumn> get $columns => [
        id,
        fullName,
        designation,
        photoPath,
        employeeId,
        email,
        phone,
        updatedAt
      ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'user_profiles';
  @override
  VerificationContext validateIntegrity(Insertable<UserProfile> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('full_name')) {
      context.handle(_fullNameMeta,
          fullName.isAcceptableOrUnknown(data['full_name']!, _fullNameMeta));
    } else if (isInserting) {
      context.missing(_fullNameMeta);
    }
    if (data.containsKey('designation')) {
      context.handle(
          _designationMeta,
          designation.isAcceptableOrUnknown(
              data['designation']!, _designationMeta));
    } else if (isInserting) {
      context.missing(_designationMeta);
    }
    if (data.containsKey('photo_path')) {
      context.handle(_photoPathMeta,
          photoPath.isAcceptableOrUnknown(data['photo_path']!, _photoPathMeta));
    }
    if (data.containsKey('employee_id')) {
      context.handle(
          _employeeIdMeta,
          employeeId.isAcceptableOrUnknown(
              data['employee_id']!, _employeeIdMeta));
    }
    if (data.containsKey('email')) {
      context.handle(
          _emailMeta, email.isAcceptableOrUnknown(data['email']!, _emailMeta));
    }
    if (data.containsKey('phone')) {
      context.handle(
          _phoneMeta, phone.isAcceptableOrUnknown(data['phone']!, _phoneMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  UserProfile map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return UserProfile(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}id'])!,
      fullName: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}full_name'])!,
      designation: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}designation'])!,
      photoPath: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}photo_path']),
      employeeId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}employee_id']),
      email: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}email']),
      phone: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}phone']),
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.dateTime, data['${effectivePrefix}updated_at'])!,
    );
  }

  @override
  $UserProfilesTable createAlias(String alias) {
    return $UserProfilesTable(attachedDatabase, alias);
  }
}

class UserProfile extends DataClass implements Insertable<UserProfile> {
  final int id;
  final String fullName;
  final String designation;
  final String? photoPath;
  final String? employeeId;
  final String? email;
  final String? phone;
  final DateTime updatedAt;
  const UserProfile(
      {required this.id,
      required this.fullName,
      required this.designation,
      this.photoPath,
      this.employeeId,
      this.email,
      this.phone,
      required this.updatedAt});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['full_name'] = Variable<String>(fullName);
    map['designation'] = Variable<String>(designation);
    if (!nullToAbsent || photoPath != null) {
      map['photo_path'] = Variable<String>(photoPath);
    }
    if (!nullToAbsent || employeeId != null) {
      map['employee_id'] = Variable<String>(employeeId);
    }
    if (!nullToAbsent || email != null) {
      map['email'] = Variable<String>(email);
    }
    if (!nullToAbsent || phone != null) {
      map['phone'] = Variable<String>(phone);
    }
    map['updated_at'] = Variable<DateTime>(updatedAt);
    return map;
  }

  UserProfilesCompanion toCompanion(bool nullToAbsent) {
    return UserProfilesCompanion(
      id: Value(id),
      fullName: Value(fullName),
      designation: Value(designation),
      photoPath: photoPath == null && nullToAbsent
          ? const Value.absent()
          : Value(photoPath),
      employeeId: employeeId == null && nullToAbsent
          ? const Value.absent()
          : Value(employeeId),
      email:
          email == null && nullToAbsent ? const Value.absent() : Value(email),
      phone:
          phone == null && nullToAbsent ? const Value.absent() : Value(phone),
      updatedAt: Value(updatedAt),
    );
  }

  factory UserProfile.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return UserProfile(
      id: serializer.fromJson<int>(json['id']),
      fullName: serializer.fromJson<String>(json['fullName']),
      designation: serializer.fromJson<String>(json['designation']),
      photoPath: serializer.fromJson<String?>(json['photoPath']),
      employeeId: serializer.fromJson<String?>(json['employeeId']),
      email: serializer.fromJson<String?>(json['email']),
      phone: serializer.fromJson<String?>(json['phone']),
      updatedAt: serializer.fromJson<DateTime>(json['updatedAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'fullName': serializer.toJson<String>(fullName),
      'designation': serializer.toJson<String>(designation),
      'photoPath': serializer.toJson<String?>(photoPath),
      'employeeId': serializer.toJson<String?>(employeeId),
      'email': serializer.toJson<String?>(email),
      'phone': serializer.toJson<String?>(phone),
      'updatedAt': serializer.toJson<DateTime>(updatedAt),
    };
  }

  UserProfile copyWith(
          {int? id,
          String? fullName,
          String? designation,
          Value<String?> photoPath = const Value.absent(),
          Value<String?> employeeId = const Value.absent(),
          Value<String?> email = const Value.absent(),
          Value<String?> phone = const Value.absent(),
          DateTime? updatedAt}) =>
      UserProfile(
        id: id ?? this.id,
        fullName: fullName ?? this.fullName,
        designation: designation ?? this.designation,
        photoPath: photoPath.present ? photoPath.value : this.photoPath,
        employeeId: employeeId.present ? employeeId.value : this.employeeId,
        email: email.present ? email.value : this.email,
        phone: phone.present ? phone.value : this.phone,
        updatedAt: updatedAt ?? this.updatedAt,
      );
  UserProfile copyWithCompanion(UserProfilesCompanion data) {
    return UserProfile(
      id: data.id.present ? data.id.value : this.id,
      fullName: data.fullName.present ? data.fullName.value : this.fullName,
      designation:
          data.designation.present ? data.designation.value : this.designation,
      photoPath: data.photoPath.present ? data.photoPath.value : this.photoPath,
      employeeId:
          data.employeeId.present ? data.employeeId.value : this.employeeId,
      email: data.email.present ? data.email.value : this.email,
      phone: data.phone.present ? data.phone.value : this.phone,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('UserProfile(')
          ..write('id: $id, ')
          ..write('fullName: $fullName, ')
          ..write('designation: $designation, ')
          ..write('photoPath: $photoPath, ')
          ..write('employeeId: $employeeId, ')
          ..write('email: $email, ')
          ..write('phone: $phone, ')
          ..write('updatedAt: $updatedAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, fullName, designation, photoPath,
      employeeId, email, phone, updatedAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is UserProfile &&
          other.id == this.id &&
          other.fullName == this.fullName &&
          other.designation == this.designation &&
          other.photoPath == this.photoPath &&
          other.employeeId == this.employeeId &&
          other.email == this.email &&
          other.phone == this.phone &&
          other.updatedAt == this.updatedAt);
}

class UserProfilesCompanion extends UpdateCompanion<UserProfile> {
  final Value<int> id;
  final Value<String> fullName;
  final Value<String> designation;
  final Value<String?> photoPath;
  final Value<String?> employeeId;
  final Value<String?> email;
  final Value<String?> phone;
  final Value<DateTime> updatedAt;
  const UserProfilesCompanion({
    this.id = const Value.absent(),
    this.fullName = const Value.absent(),
    this.designation = const Value.absent(),
    this.photoPath = const Value.absent(),
    this.employeeId = const Value.absent(),
    this.email = const Value.absent(),
    this.phone = const Value.absent(),
    this.updatedAt = const Value.absent(),
  });
  UserProfilesCompanion.insert({
    this.id = const Value.absent(),
    required String fullName,
    required String designation,
    this.photoPath = const Value.absent(),
    this.employeeId = const Value.absent(),
    this.email = const Value.absent(),
    this.phone = const Value.absent(),
    this.updatedAt = const Value.absent(),
  })  : fullName = Value(fullName),
        designation = Value(designation);
  static Insertable<UserProfile> custom({
    Expression<int>? id,
    Expression<String>? fullName,
    Expression<String>? designation,
    Expression<String>? photoPath,
    Expression<String>? employeeId,
    Expression<String>? email,
    Expression<String>? phone,
    Expression<DateTime>? updatedAt,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (fullName != null) 'full_name': fullName,
      if (designation != null) 'designation': designation,
      if (photoPath != null) 'photo_path': photoPath,
      if (employeeId != null) 'employee_id': employeeId,
      if (email != null) 'email': email,
      if (phone != null) 'phone': phone,
      if (updatedAt != null) 'updated_at': updatedAt,
    });
  }

  UserProfilesCompanion copyWith(
      {Value<int>? id,
      Value<String>? fullName,
      Value<String>? designation,
      Value<String?>? photoPath,
      Value<String?>? employeeId,
      Value<String?>? email,
      Value<String?>? phone,
      Value<DateTime>? updatedAt}) {
    return UserProfilesCompanion(
      id: id ?? this.id,
      fullName: fullName ?? this.fullName,
      designation: designation ?? this.designation,
      photoPath: photoPath ?? this.photoPath,
      employeeId: employeeId ?? this.employeeId,
      email: email ?? this.email,
      phone: phone ?? this.phone,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (fullName.present) {
      map['full_name'] = Variable<String>(fullName.value);
    }
    if (designation.present) {
      map['designation'] = Variable<String>(designation.value);
    }
    if (photoPath.present) {
      map['photo_path'] = Variable<String>(photoPath.value);
    }
    if (employeeId.present) {
      map['employee_id'] = Variable<String>(employeeId.value);
    }
    if (email.present) {
      map['email'] = Variable<String>(email.value);
    }
    if (phone.present) {
      map['phone'] = Variable<String>(phone.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<DateTime>(updatedAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('UserProfilesCompanion(')
          ..write('id: $id, ')
          ..write('fullName: $fullName, ')
          ..write('designation: $designation, ')
          ..write('photoPath: $photoPath, ')
          ..write('employeeId: $employeeId, ')
          ..write('email: $email, ')
          ..write('phone: $phone, ')
          ..write('updatedAt: $updatedAt')
          ..write(')'))
        .toString();
  }
}

class $NotificationLogsTable extends NotificationLogs
    with TableInfo<$NotificationLogsTable, NotificationLog> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $NotificationLogsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
      'id', aliasedName, false,
      hasAutoIncrement: true,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultConstraints:
          GeneratedColumn.constraintIsAlways('PRIMARY KEY AUTOINCREMENT'));
  static const VerificationMeta _typeMeta = const VerificationMeta('type');
  @override
  late final GeneratedColumn<String> type = GeneratedColumn<String>(
      'type', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _triggeredAtMeta =
      const VerificationMeta('triggeredAt');
  @override
  late final GeneratedColumn<DateTime> triggeredAt = GeneratedColumn<DateTime>(
      'triggered_at', aliasedName, false,
      type: DriftSqlType.dateTime, requiredDuringInsert: true);
  static const VerificationMeta _dateYyyyMmDdMeta =
      const VerificationMeta('dateYyyyMmDd');
  @override
  late final GeneratedColumn<String> dateYyyyMmDd = GeneratedColumn<String>(
      'date_yyyy_mm_dd', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  @override
  List<GeneratedColumn> get $columns => [id, type, triggeredAt, dateYyyyMmDd];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'notification_logs';
  @override
  VerificationContext validateIntegrity(Insertable<NotificationLog> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('type')) {
      context.handle(
          _typeMeta, type.isAcceptableOrUnknown(data['type']!, _typeMeta));
    } else if (isInserting) {
      context.missing(_typeMeta);
    }
    if (data.containsKey('triggered_at')) {
      context.handle(
          _triggeredAtMeta,
          triggeredAt.isAcceptableOrUnknown(
              data['triggered_at']!, _triggeredAtMeta));
    } else if (isInserting) {
      context.missing(_triggeredAtMeta);
    }
    if (data.containsKey('date_yyyy_mm_dd')) {
      context.handle(
          _dateYyyyMmDdMeta,
          dateYyyyMmDd.isAcceptableOrUnknown(
              data['date_yyyy_mm_dd']!, _dateYyyyMmDdMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  NotificationLog map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return NotificationLog(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}id'])!,
      type: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}type'])!,
      triggeredAt: attachedDatabase.typeMapping
          .read(DriftSqlType.dateTime, data['${effectivePrefix}triggered_at'])!,
      dateYyyyMmDd: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}date_yyyy_mm_dd']),
    );
  }

  @override
  $NotificationLogsTable createAlias(String alias) {
    return $NotificationLogsTable(attachedDatabase, alias);
  }
}

class NotificationLog extends DataClass implements Insertable<NotificationLog> {
  final int id;
  final String type;
  final DateTime triggeredAt;
  final String? dateYyyyMmDd;
  const NotificationLog(
      {required this.id,
      required this.type,
      required this.triggeredAt,
      this.dateYyyyMmDd});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['type'] = Variable<String>(type);
    map['triggered_at'] = Variable<DateTime>(triggeredAt);
    if (!nullToAbsent || dateYyyyMmDd != null) {
      map['date_yyyy_mm_dd'] = Variable<String>(dateYyyyMmDd);
    }
    return map;
  }

  NotificationLogsCompanion toCompanion(bool nullToAbsent) {
    return NotificationLogsCompanion(
      id: Value(id),
      type: Value(type),
      triggeredAt: Value(triggeredAt),
      dateYyyyMmDd: dateYyyyMmDd == null && nullToAbsent
          ? const Value.absent()
          : Value(dateYyyyMmDd),
    );
  }

  factory NotificationLog.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return NotificationLog(
      id: serializer.fromJson<int>(json['id']),
      type: serializer.fromJson<String>(json['type']),
      triggeredAt: serializer.fromJson<DateTime>(json['triggeredAt']),
      dateYyyyMmDd: serializer.fromJson<String?>(json['dateYyyyMmDd']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'type': serializer.toJson<String>(type),
      'triggeredAt': serializer.toJson<DateTime>(triggeredAt),
      'dateYyyyMmDd': serializer.toJson<String?>(dateYyyyMmDd),
    };
  }

  NotificationLog copyWith(
          {int? id,
          String? type,
          DateTime? triggeredAt,
          Value<String?> dateYyyyMmDd = const Value.absent()}) =>
      NotificationLog(
        id: id ?? this.id,
        type: type ?? this.type,
        triggeredAt: triggeredAt ?? this.triggeredAt,
        dateYyyyMmDd:
            dateYyyyMmDd.present ? dateYyyyMmDd.value : this.dateYyyyMmDd,
      );
  NotificationLog copyWithCompanion(NotificationLogsCompanion data) {
    return NotificationLog(
      id: data.id.present ? data.id.value : this.id,
      type: data.type.present ? data.type.value : this.type,
      triggeredAt:
          data.triggeredAt.present ? data.triggeredAt.value : this.triggeredAt,
      dateYyyyMmDd: data.dateYyyyMmDd.present
          ? data.dateYyyyMmDd.value
          : this.dateYyyyMmDd,
    );
  }

  @override
  String toString() {
    return (StringBuffer('NotificationLog(')
          ..write('id: $id, ')
          ..write('type: $type, ')
          ..write('triggeredAt: $triggeredAt, ')
          ..write('dateYyyyMmDd: $dateYyyyMmDd')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, type, triggeredAt, dateYyyyMmDd);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is NotificationLog &&
          other.id == this.id &&
          other.type == this.type &&
          other.triggeredAt == this.triggeredAt &&
          other.dateYyyyMmDd == this.dateYyyyMmDd);
}

class NotificationLogsCompanion extends UpdateCompanion<NotificationLog> {
  final Value<int> id;
  final Value<String> type;
  final Value<DateTime> triggeredAt;
  final Value<String?> dateYyyyMmDd;
  const NotificationLogsCompanion({
    this.id = const Value.absent(),
    this.type = const Value.absent(),
    this.triggeredAt = const Value.absent(),
    this.dateYyyyMmDd = const Value.absent(),
  });
  NotificationLogsCompanion.insert({
    this.id = const Value.absent(),
    required String type,
    required DateTime triggeredAt,
    this.dateYyyyMmDd = const Value.absent(),
  })  : type = Value(type),
        triggeredAt = Value(triggeredAt);
  static Insertable<NotificationLog> custom({
    Expression<int>? id,
    Expression<String>? type,
    Expression<DateTime>? triggeredAt,
    Expression<String>? dateYyyyMmDd,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (type != null) 'type': type,
      if (triggeredAt != null) 'triggered_at': triggeredAt,
      if (dateYyyyMmDd != null) 'date_yyyy_mm_dd': dateYyyyMmDd,
    });
  }

  NotificationLogsCompanion copyWith(
      {Value<int>? id,
      Value<String>? type,
      Value<DateTime>? triggeredAt,
      Value<String?>? dateYyyyMmDd}) {
    return NotificationLogsCompanion(
      id: id ?? this.id,
      type: type ?? this.type,
      triggeredAt: triggeredAt ?? this.triggeredAt,
      dateYyyyMmDd: dateYyyyMmDd ?? this.dateYyyyMmDd,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (type.present) {
      map['type'] = Variable<String>(type.value);
    }
    if (triggeredAt.present) {
      map['triggered_at'] = Variable<DateTime>(triggeredAt.value);
    }
    if (dateYyyyMmDd.present) {
      map['date_yyyy_mm_dd'] = Variable<String>(dateYyyyMmDd.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('NotificationLogsCompanion(')
          ..write('id: $id, ')
          ..write('type: $type, ')
          ..write('triggeredAt: $triggeredAt, ')
          ..write('dateYyyyMmDd: $dateYyyyMmDd')
          ..write(')'))
        .toString();
  }
}

class $WfoScheduleHistoryTable extends WfoScheduleHistory
    with TableInfo<$WfoScheduleHistoryTable, WfoScheduleHistoryEntry> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $WfoScheduleHistoryTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
      'id', aliasedName, false,
      hasAutoIncrement: true,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultConstraints:
          GeneratedColumn.constraintIsAlways('PRIMARY KEY AUTOINCREMENT'));
  static const VerificationMeta _wfoDaysMaskMeta =
      const VerificationMeta('wfoDaysMask');
  @override
  late final GeneratedColumn<int> wfoDaysMask = GeneratedColumn<int>(
      'wfo_days_mask', aliasedName, false,
      type: DriftSqlType.int, requiredDuringInsert: true);
  static const VerificationMeta _effectiveFromMeta =
      const VerificationMeta('effectiveFrom');
  @override
  late final GeneratedColumn<DateTime> effectiveFrom =
      GeneratedColumn<DateTime>('effective_from', aliasedName, false,
          type: DriftSqlType.dateTime, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns => [id, wfoDaysMask, effectiveFrom];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'wfo_schedule_history';
  @override
  VerificationContext validateIntegrity(
      Insertable<WfoScheduleHistoryEntry> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('wfo_days_mask')) {
      context.handle(
          _wfoDaysMaskMeta,
          wfoDaysMask.isAcceptableOrUnknown(
              data['wfo_days_mask']!, _wfoDaysMaskMeta));
    } else if (isInserting) {
      context.missing(_wfoDaysMaskMeta);
    }
    if (data.containsKey('effective_from')) {
      context.handle(
          _effectiveFromMeta,
          effectiveFrom.isAcceptableOrUnknown(
              data['effective_from']!, _effectiveFromMeta));
    } else if (isInserting) {
      context.missing(_effectiveFromMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  WfoScheduleHistoryEntry map(Map<String, dynamic> data,
      {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return WfoScheduleHistoryEntry(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}id'])!,
      wfoDaysMask: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}wfo_days_mask'])!,
      effectiveFrom: attachedDatabase.typeMapping.read(
          DriftSqlType.dateTime, data['${effectivePrefix}effective_from'])!,
    );
  }

  @override
  $WfoScheduleHistoryTable createAlias(String alias) {
    return $WfoScheduleHistoryTable(attachedDatabase, alias);
  }
}

class WfoScheduleHistoryEntry extends DataClass
    implements Insertable<WfoScheduleHistoryEntry> {
  final int id;
  final int wfoDaysMask;
  final DateTime effectiveFrom;
  const WfoScheduleHistoryEntry(
      {required this.id,
      required this.wfoDaysMask,
      required this.effectiveFrom});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['wfo_days_mask'] = Variable<int>(wfoDaysMask);
    map['effective_from'] = Variable<DateTime>(effectiveFrom);
    return map;
  }

  WfoScheduleHistoryCompanion toCompanion(bool nullToAbsent) {
    return WfoScheduleHistoryCompanion(
      id: Value(id),
      wfoDaysMask: Value(wfoDaysMask),
      effectiveFrom: Value(effectiveFrom),
    );
  }

  factory WfoScheduleHistoryEntry.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return WfoScheduleHistoryEntry(
      id: serializer.fromJson<int>(json['id']),
      wfoDaysMask: serializer.fromJson<int>(json['wfoDaysMask']),
      effectiveFrom: serializer.fromJson<DateTime>(json['effectiveFrom']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'wfoDaysMask': serializer.toJson<int>(wfoDaysMask),
      'effectiveFrom': serializer.toJson<DateTime>(effectiveFrom),
    };
  }

  WfoScheduleHistoryEntry copyWith(
          {int? id, int? wfoDaysMask, DateTime? effectiveFrom}) =>
      WfoScheduleHistoryEntry(
        id: id ?? this.id,
        wfoDaysMask: wfoDaysMask ?? this.wfoDaysMask,
        effectiveFrom: effectiveFrom ?? this.effectiveFrom,
      );
  WfoScheduleHistoryEntry copyWithCompanion(WfoScheduleHistoryCompanion data) {
    return WfoScheduleHistoryEntry(
      id: data.id.present ? data.id.value : this.id,
      wfoDaysMask:
          data.wfoDaysMask.present ? data.wfoDaysMask.value : this.wfoDaysMask,
      effectiveFrom: data.effectiveFrom.present
          ? data.effectiveFrom.value
          : this.effectiveFrom,
    );
  }

  @override
  String toString() {
    return (StringBuffer('WfoScheduleHistoryEntry(')
          ..write('id: $id, ')
          ..write('wfoDaysMask: $wfoDaysMask, ')
          ..write('effectiveFrom: $effectiveFrom')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, wfoDaysMask, effectiveFrom);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is WfoScheduleHistoryEntry &&
          other.id == this.id &&
          other.wfoDaysMask == this.wfoDaysMask &&
          other.effectiveFrom == this.effectiveFrom);
}

class WfoScheduleHistoryCompanion
    extends UpdateCompanion<WfoScheduleHistoryEntry> {
  final Value<int> id;
  final Value<int> wfoDaysMask;
  final Value<DateTime> effectiveFrom;
  const WfoScheduleHistoryCompanion({
    this.id = const Value.absent(),
    this.wfoDaysMask = const Value.absent(),
    this.effectiveFrom = const Value.absent(),
  });
  WfoScheduleHistoryCompanion.insert({
    this.id = const Value.absent(),
    required int wfoDaysMask,
    required DateTime effectiveFrom,
  })  : wfoDaysMask = Value(wfoDaysMask),
        effectiveFrom = Value(effectiveFrom);
  static Insertable<WfoScheduleHistoryEntry> custom({
    Expression<int>? id,
    Expression<int>? wfoDaysMask,
    Expression<DateTime>? effectiveFrom,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (wfoDaysMask != null) 'wfo_days_mask': wfoDaysMask,
      if (effectiveFrom != null) 'effective_from': effectiveFrom,
    });
  }

  WfoScheduleHistoryCompanion copyWith(
      {Value<int>? id,
      Value<int>? wfoDaysMask,
      Value<DateTime>? effectiveFrom}) {
    return WfoScheduleHistoryCompanion(
      id: id ?? this.id,
      wfoDaysMask: wfoDaysMask ?? this.wfoDaysMask,
      effectiveFrom: effectiveFrom ?? this.effectiveFrom,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (wfoDaysMask.present) {
      map['wfo_days_mask'] = Variable<int>(wfoDaysMask.value);
    }
    if (effectiveFrom.present) {
      map['effective_from'] = Variable<DateTime>(effectiveFrom.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('WfoScheduleHistoryCompanion(')
          ..write('id: $id, ')
          ..write('wfoDaysMask: $wfoDaysMask, ')
          ..write('effectiveFrom: $effectiveFrom')
          ..write(')'))
        .toString();
  }
}

abstract class _$AppDatabase extends GeneratedDatabase {
  _$AppDatabase(QueryExecutor e) : super(e);
  $AppDatabaseManager get managers => $AppDatabaseManager(this);
  late final $OfficeConfigsTable officeConfigs = $OfficeConfigsTable(this);
  late final $AttendanceRecordsTable attendanceRecords =
      $AttendanceRecordsTable(this);
  late final $UserProfilesTable userProfiles = $UserProfilesTable(this);
  late final $NotificationLogsTable notificationLogs =
      $NotificationLogsTable(this);
  late final $WfoScheduleHistoryTable wfoScheduleHistory =
      $WfoScheduleHistoryTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [
        officeConfigs,
        attendanceRecords,
        userProfiles,
        notificationLogs,
        wfoScheduleHistory
      ];
}

typedef $$OfficeConfigsTableCreateCompanionBuilder = OfficeConfigsCompanion
    Function({
  Value<int> id,
  required String ssid,
  required double latitude,
  required double longitude,
  Value<int> radiusMeters,
  Value<String> lateCutoffTime,
  Value<String> checkInTime,
  Value<String> checkOutTime,
  Value<String> portalUrl,
  Value<int> workingDaysMask,
  Value<int> wfoDaysMask,
  Value<DateTime> createdAt,
  Value<DateTime> updatedAt,
});
typedef $$OfficeConfigsTableUpdateCompanionBuilder = OfficeConfigsCompanion
    Function({
  Value<int> id,
  Value<String> ssid,
  Value<double> latitude,
  Value<double> longitude,
  Value<int> radiusMeters,
  Value<String> lateCutoffTime,
  Value<String> checkInTime,
  Value<String> checkOutTime,
  Value<String> portalUrl,
  Value<int> workingDaysMask,
  Value<int> wfoDaysMask,
  Value<DateTime> createdAt,
  Value<DateTime> updatedAt,
});

class $$OfficeConfigsTableFilterComposer
    extends Composer<_$AppDatabase, $OfficeConfigsTable> {
  $$OfficeConfigsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get ssid => $composableBuilder(
      column: $table.ssid, builder: (column) => ColumnFilters(column));

  ColumnFilters<double> get latitude => $composableBuilder(
      column: $table.latitude, builder: (column) => ColumnFilters(column));

  ColumnFilters<double> get longitude => $composableBuilder(
      column: $table.longitude, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get radiusMeters => $composableBuilder(
      column: $table.radiusMeters, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get lateCutoffTime => $composableBuilder(
      column: $table.lateCutoffTime,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get checkInTime => $composableBuilder(
      column: $table.checkInTime, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get checkOutTime => $composableBuilder(
      column: $table.checkOutTime, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get portalUrl => $composableBuilder(
      column: $table.portalUrl, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get workingDaysMask => $composableBuilder(
      column: $table.workingDaysMask,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get wfoDaysMask => $composableBuilder(
      column: $table.wfoDaysMask, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get createdAt => $composableBuilder(
      column: $table.createdAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));
}

class $$OfficeConfigsTableOrderingComposer
    extends Composer<_$AppDatabase, $OfficeConfigsTable> {
  $$OfficeConfigsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get ssid => $composableBuilder(
      column: $table.ssid, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<double> get latitude => $composableBuilder(
      column: $table.latitude, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<double> get longitude => $composableBuilder(
      column: $table.longitude, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get radiusMeters => $composableBuilder(
      column: $table.radiusMeters,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get lateCutoffTime => $composableBuilder(
      column: $table.lateCutoffTime,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get checkInTime => $composableBuilder(
      column: $table.checkInTime, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get checkOutTime => $composableBuilder(
      column: $table.checkOutTime,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get portalUrl => $composableBuilder(
      column: $table.portalUrl, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get workingDaysMask => $composableBuilder(
      column: $table.workingDaysMask,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get wfoDaysMask => $composableBuilder(
      column: $table.wfoDaysMask, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get createdAt => $composableBuilder(
      column: $table.createdAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));
}

class $$OfficeConfigsTableAnnotationComposer
    extends Composer<_$AppDatabase, $OfficeConfigsTable> {
  $$OfficeConfigsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get ssid =>
      $composableBuilder(column: $table.ssid, builder: (column) => column);

  GeneratedColumn<double> get latitude =>
      $composableBuilder(column: $table.latitude, builder: (column) => column);

  GeneratedColumn<double> get longitude =>
      $composableBuilder(column: $table.longitude, builder: (column) => column);

  GeneratedColumn<int> get radiusMeters => $composableBuilder(
      column: $table.radiusMeters, builder: (column) => column);

  GeneratedColumn<String> get lateCutoffTime => $composableBuilder(
      column: $table.lateCutoffTime, builder: (column) => column);

  GeneratedColumn<String> get checkInTime => $composableBuilder(
      column: $table.checkInTime, builder: (column) => column);

  GeneratedColumn<String> get checkOutTime => $composableBuilder(
      column: $table.checkOutTime, builder: (column) => column);

  GeneratedColumn<String> get portalUrl =>
      $composableBuilder(column: $table.portalUrl, builder: (column) => column);

  GeneratedColumn<int> get workingDaysMask => $composableBuilder(
      column: $table.workingDaysMask, builder: (column) => column);

  GeneratedColumn<int> get wfoDaysMask => $composableBuilder(
      column: $table.wfoDaysMask, builder: (column) => column);

  GeneratedColumn<DateTime> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);

  GeneratedColumn<DateTime> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);
}

class $$OfficeConfigsTableTableManager extends RootTableManager<
    _$AppDatabase,
    $OfficeConfigsTable,
    OfficeConfig,
    $$OfficeConfigsTableFilterComposer,
    $$OfficeConfigsTableOrderingComposer,
    $$OfficeConfigsTableAnnotationComposer,
    $$OfficeConfigsTableCreateCompanionBuilder,
    $$OfficeConfigsTableUpdateCompanionBuilder,
    (
      OfficeConfig,
      BaseReferences<_$AppDatabase, $OfficeConfigsTable, OfficeConfig>
    ),
    OfficeConfig,
    PrefetchHooks Function()> {
  $$OfficeConfigsTableTableManager(_$AppDatabase db, $OfficeConfigsTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$OfficeConfigsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$OfficeConfigsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$OfficeConfigsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<int> id = const Value.absent(),
            Value<String> ssid = const Value.absent(),
            Value<double> latitude = const Value.absent(),
            Value<double> longitude = const Value.absent(),
            Value<int> radiusMeters = const Value.absent(),
            Value<String> lateCutoffTime = const Value.absent(),
            Value<String> checkInTime = const Value.absent(),
            Value<String> checkOutTime = const Value.absent(),
            Value<String> portalUrl = const Value.absent(),
            Value<int> workingDaysMask = const Value.absent(),
            Value<int> wfoDaysMask = const Value.absent(),
            Value<DateTime> createdAt = const Value.absent(),
            Value<DateTime> updatedAt = const Value.absent(),
          }) =>
              OfficeConfigsCompanion(
            id: id,
            ssid: ssid,
            latitude: latitude,
            longitude: longitude,
            radiusMeters: radiusMeters,
            lateCutoffTime: lateCutoffTime,
            checkInTime: checkInTime,
            checkOutTime: checkOutTime,
            portalUrl: portalUrl,
            workingDaysMask: workingDaysMask,
            wfoDaysMask: wfoDaysMask,
            createdAt: createdAt,
            updatedAt: updatedAt,
          ),
          createCompanionCallback: ({
            Value<int> id = const Value.absent(),
            required String ssid,
            required double latitude,
            required double longitude,
            Value<int> radiusMeters = const Value.absent(),
            Value<String> lateCutoffTime = const Value.absent(),
            Value<String> checkInTime = const Value.absent(),
            Value<String> checkOutTime = const Value.absent(),
            Value<String> portalUrl = const Value.absent(),
            Value<int> workingDaysMask = const Value.absent(),
            Value<int> wfoDaysMask = const Value.absent(),
            Value<DateTime> createdAt = const Value.absent(),
            Value<DateTime> updatedAt = const Value.absent(),
          }) =>
              OfficeConfigsCompanion.insert(
            id: id,
            ssid: ssid,
            latitude: latitude,
            longitude: longitude,
            radiusMeters: radiusMeters,
            lateCutoffTime: lateCutoffTime,
            checkInTime: checkInTime,
            checkOutTime: checkOutTime,
            portalUrl: portalUrl,
            workingDaysMask: workingDaysMask,
            wfoDaysMask: wfoDaysMask,
            createdAt: createdAt,
            updatedAt: updatedAt,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$OfficeConfigsTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $OfficeConfigsTable,
    OfficeConfig,
    $$OfficeConfigsTableFilterComposer,
    $$OfficeConfigsTableOrderingComposer,
    $$OfficeConfigsTableAnnotationComposer,
    $$OfficeConfigsTableCreateCompanionBuilder,
    $$OfficeConfigsTableUpdateCompanionBuilder,
    (
      OfficeConfig,
      BaseReferences<_$AppDatabase, $OfficeConfigsTable, OfficeConfig>
    ),
    OfficeConfig,
    PrefetchHooks Function()>;
typedef $$AttendanceRecordsTableCreateCompanionBuilder
    = AttendanceRecordsCompanion Function({
  Value<int> id,
  required String dateYyyyMmDd,
  required AttendanceStatus status,
  required DateTime markedAt,
  Value<String?> ssidSnapshot,
  Value<double?> distanceMeters,
});
typedef $$AttendanceRecordsTableUpdateCompanionBuilder
    = AttendanceRecordsCompanion Function({
  Value<int> id,
  Value<String> dateYyyyMmDd,
  Value<AttendanceStatus> status,
  Value<DateTime> markedAt,
  Value<String?> ssidSnapshot,
  Value<double?> distanceMeters,
});

class $$AttendanceRecordsTableFilterComposer
    extends Composer<_$AppDatabase, $AttendanceRecordsTable> {
  $$AttendanceRecordsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get dateYyyyMmDd => $composableBuilder(
      column: $table.dateYyyyMmDd, builder: (column) => ColumnFilters(column));

  ColumnWithTypeConverterFilters<AttendanceStatus, AttendanceStatus, String>
      get status => $composableBuilder(
          column: $table.status,
          builder: (column) => ColumnWithTypeConverterFilters(column));

  ColumnFilters<DateTime> get markedAt => $composableBuilder(
      column: $table.markedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get ssidSnapshot => $composableBuilder(
      column: $table.ssidSnapshot, builder: (column) => ColumnFilters(column));

  ColumnFilters<double> get distanceMeters => $composableBuilder(
      column: $table.distanceMeters,
      builder: (column) => ColumnFilters(column));
}

class $$AttendanceRecordsTableOrderingComposer
    extends Composer<_$AppDatabase, $AttendanceRecordsTable> {
  $$AttendanceRecordsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get dateYyyyMmDd => $composableBuilder(
      column: $table.dateYyyyMmDd,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get status => $composableBuilder(
      column: $table.status, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get markedAt => $composableBuilder(
      column: $table.markedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get ssidSnapshot => $composableBuilder(
      column: $table.ssidSnapshot,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<double> get distanceMeters => $composableBuilder(
      column: $table.distanceMeters,
      builder: (column) => ColumnOrderings(column));
}

class $$AttendanceRecordsTableAnnotationComposer
    extends Composer<_$AppDatabase, $AttendanceRecordsTable> {
  $$AttendanceRecordsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get dateYyyyMmDd => $composableBuilder(
      column: $table.dateYyyyMmDd, builder: (column) => column);

  GeneratedColumnWithTypeConverter<AttendanceStatus, String> get status =>
      $composableBuilder(column: $table.status, builder: (column) => column);

  GeneratedColumn<DateTime> get markedAt =>
      $composableBuilder(column: $table.markedAt, builder: (column) => column);

  GeneratedColumn<String> get ssidSnapshot => $composableBuilder(
      column: $table.ssidSnapshot, builder: (column) => column);

  GeneratedColumn<double> get distanceMeters => $composableBuilder(
      column: $table.distanceMeters, builder: (column) => column);
}

class $$AttendanceRecordsTableTableManager extends RootTableManager<
    _$AppDatabase,
    $AttendanceRecordsTable,
    AttendanceRecord,
    $$AttendanceRecordsTableFilterComposer,
    $$AttendanceRecordsTableOrderingComposer,
    $$AttendanceRecordsTableAnnotationComposer,
    $$AttendanceRecordsTableCreateCompanionBuilder,
    $$AttendanceRecordsTableUpdateCompanionBuilder,
    (
      AttendanceRecord,
      BaseReferences<_$AppDatabase, $AttendanceRecordsTable, AttendanceRecord>
    ),
    AttendanceRecord,
    PrefetchHooks Function()> {
  $$AttendanceRecordsTableTableManager(
      _$AppDatabase db, $AttendanceRecordsTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$AttendanceRecordsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$AttendanceRecordsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$AttendanceRecordsTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<int> id = const Value.absent(),
            Value<String> dateYyyyMmDd = const Value.absent(),
            Value<AttendanceStatus> status = const Value.absent(),
            Value<DateTime> markedAt = const Value.absent(),
            Value<String?> ssidSnapshot = const Value.absent(),
            Value<double?> distanceMeters = const Value.absent(),
          }) =>
              AttendanceRecordsCompanion(
            id: id,
            dateYyyyMmDd: dateYyyyMmDd,
            status: status,
            markedAt: markedAt,
            ssidSnapshot: ssidSnapshot,
            distanceMeters: distanceMeters,
          ),
          createCompanionCallback: ({
            Value<int> id = const Value.absent(),
            required String dateYyyyMmDd,
            required AttendanceStatus status,
            required DateTime markedAt,
            Value<String?> ssidSnapshot = const Value.absent(),
            Value<double?> distanceMeters = const Value.absent(),
          }) =>
              AttendanceRecordsCompanion.insert(
            id: id,
            dateYyyyMmDd: dateYyyyMmDd,
            status: status,
            markedAt: markedAt,
            ssidSnapshot: ssidSnapshot,
            distanceMeters: distanceMeters,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$AttendanceRecordsTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $AttendanceRecordsTable,
    AttendanceRecord,
    $$AttendanceRecordsTableFilterComposer,
    $$AttendanceRecordsTableOrderingComposer,
    $$AttendanceRecordsTableAnnotationComposer,
    $$AttendanceRecordsTableCreateCompanionBuilder,
    $$AttendanceRecordsTableUpdateCompanionBuilder,
    (
      AttendanceRecord,
      BaseReferences<_$AppDatabase, $AttendanceRecordsTable, AttendanceRecord>
    ),
    AttendanceRecord,
    PrefetchHooks Function()>;
typedef $$UserProfilesTableCreateCompanionBuilder = UserProfilesCompanion
    Function({
  Value<int> id,
  required String fullName,
  required String designation,
  Value<String?> photoPath,
  Value<String?> employeeId,
  Value<String?> email,
  Value<String?> phone,
  Value<DateTime> updatedAt,
});
typedef $$UserProfilesTableUpdateCompanionBuilder = UserProfilesCompanion
    Function({
  Value<int> id,
  Value<String> fullName,
  Value<String> designation,
  Value<String?> photoPath,
  Value<String?> employeeId,
  Value<String?> email,
  Value<String?> phone,
  Value<DateTime> updatedAt,
});

class $$UserProfilesTableFilterComposer
    extends Composer<_$AppDatabase, $UserProfilesTable> {
  $$UserProfilesTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get fullName => $composableBuilder(
      column: $table.fullName, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get designation => $composableBuilder(
      column: $table.designation, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get photoPath => $composableBuilder(
      column: $table.photoPath, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get employeeId => $composableBuilder(
      column: $table.employeeId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get email => $composableBuilder(
      column: $table.email, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get phone => $composableBuilder(
      column: $table.phone, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));
}

class $$UserProfilesTableOrderingComposer
    extends Composer<_$AppDatabase, $UserProfilesTable> {
  $$UserProfilesTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get fullName => $composableBuilder(
      column: $table.fullName, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get designation => $composableBuilder(
      column: $table.designation, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get photoPath => $composableBuilder(
      column: $table.photoPath, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get employeeId => $composableBuilder(
      column: $table.employeeId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get email => $composableBuilder(
      column: $table.email, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get phone => $composableBuilder(
      column: $table.phone, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));
}

class $$UserProfilesTableAnnotationComposer
    extends Composer<_$AppDatabase, $UserProfilesTable> {
  $$UserProfilesTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get fullName =>
      $composableBuilder(column: $table.fullName, builder: (column) => column);

  GeneratedColumn<String> get designation => $composableBuilder(
      column: $table.designation, builder: (column) => column);

  GeneratedColumn<String> get photoPath =>
      $composableBuilder(column: $table.photoPath, builder: (column) => column);

  GeneratedColumn<String> get employeeId => $composableBuilder(
      column: $table.employeeId, builder: (column) => column);

  GeneratedColumn<String> get email =>
      $composableBuilder(column: $table.email, builder: (column) => column);

  GeneratedColumn<String> get phone =>
      $composableBuilder(column: $table.phone, builder: (column) => column);

  GeneratedColumn<DateTime> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);
}

class $$UserProfilesTableTableManager extends RootTableManager<
    _$AppDatabase,
    $UserProfilesTable,
    UserProfile,
    $$UserProfilesTableFilterComposer,
    $$UserProfilesTableOrderingComposer,
    $$UserProfilesTableAnnotationComposer,
    $$UserProfilesTableCreateCompanionBuilder,
    $$UserProfilesTableUpdateCompanionBuilder,
    (
      UserProfile,
      BaseReferences<_$AppDatabase, $UserProfilesTable, UserProfile>
    ),
    UserProfile,
    PrefetchHooks Function()> {
  $$UserProfilesTableTableManager(_$AppDatabase db, $UserProfilesTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$UserProfilesTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$UserProfilesTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$UserProfilesTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<int> id = const Value.absent(),
            Value<String> fullName = const Value.absent(),
            Value<String> designation = const Value.absent(),
            Value<String?> photoPath = const Value.absent(),
            Value<String?> employeeId = const Value.absent(),
            Value<String?> email = const Value.absent(),
            Value<String?> phone = const Value.absent(),
            Value<DateTime> updatedAt = const Value.absent(),
          }) =>
              UserProfilesCompanion(
            id: id,
            fullName: fullName,
            designation: designation,
            photoPath: photoPath,
            employeeId: employeeId,
            email: email,
            phone: phone,
            updatedAt: updatedAt,
          ),
          createCompanionCallback: ({
            Value<int> id = const Value.absent(),
            required String fullName,
            required String designation,
            Value<String?> photoPath = const Value.absent(),
            Value<String?> employeeId = const Value.absent(),
            Value<String?> email = const Value.absent(),
            Value<String?> phone = const Value.absent(),
            Value<DateTime> updatedAt = const Value.absent(),
          }) =>
              UserProfilesCompanion.insert(
            id: id,
            fullName: fullName,
            designation: designation,
            photoPath: photoPath,
            employeeId: employeeId,
            email: email,
            phone: phone,
            updatedAt: updatedAt,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$UserProfilesTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $UserProfilesTable,
    UserProfile,
    $$UserProfilesTableFilterComposer,
    $$UserProfilesTableOrderingComposer,
    $$UserProfilesTableAnnotationComposer,
    $$UserProfilesTableCreateCompanionBuilder,
    $$UserProfilesTableUpdateCompanionBuilder,
    (
      UserProfile,
      BaseReferences<_$AppDatabase, $UserProfilesTable, UserProfile>
    ),
    UserProfile,
    PrefetchHooks Function()>;
typedef $$NotificationLogsTableCreateCompanionBuilder
    = NotificationLogsCompanion Function({
  Value<int> id,
  required String type,
  required DateTime triggeredAt,
  Value<String?> dateYyyyMmDd,
});
typedef $$NotificationLogsTableUpdateCompanionBuilder
    = NotificationLogsCompanion Function({
  Value<int> id,
  Value<String> type,
  Value<DateTime> triggeredAt,
  Value<String?> dateYyyyMmDd,
});

class $$NotificationLogsTableFilterComposer
    extends Composer<_$AppDatabase, $NotificationLogsTable> {
  $$NotificationLogsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get type => $composableBuilder(
      column: $table.type, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get triggeredAt => $composableBuilder(
      column: $table.triggeredAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get dateYyyyMmDd => $composableBuilder(
      column: $table.dateYyyyMmDd, builder: (column) => ColumnFilters(column));
}

class $$NotificationLogsTableOrderingComposer
    extends Composer<_$AppDatabase, $NotificationLogsTable> {
  $$NotificationLogsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get type => $composableBuilder(
      column: $table.type, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get triggeredAt => $composableBuilder(
      column: $table.triggeredAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get dateYyyyMmDd => $composableBuilder(
      column: $table.dateYyyyMmDd,
      builder: (column) => ColumnOrderings(column));
}

class $$NotificationLogsTableAnnotationComposer
    extends Composer<_$AppDatabase, $NotificationLogsTable> {
  $$NotificationLogsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get type =>
      $composableBuilder(column: $table.type, builder: (column) => column);

  GeneratedColumn<DateTime> get triggeredAt => $composableBuilder(
      column: $table.triggeredAt, builder: (column) => column);

  GeneratedColumn<String> get dateYyyyMmDd => $composableBuilder(
      column: $table.dateYyyyMmDd, builder: (column) => column);
}

class $$NotificationLogsTableTableManager extends RootTableManager<
    _$AppDatabase,
    $NotificationLogsTable,
    NotificationLog,
    $$NotificationLogsTableFilterComposer,
    $$NotificationLogsTableOrderingComposer,
    $$NotificationLogsTableAnnotationComposer,
    $$NotificationLogsTableCreateCompanionBuilder,
    $$NotificationLogsTableUpdateCompanionBuilder,
    (
      NotificationLog,
      BaseReferences<_$AppDatabase, $NotificationLogsTable, NotificationLog>
    ),
    NotificationLog,
    PrefetchHooks Function()> {
  $$NotificationLogsTableTableManager(
      _$AppDatabase db, $NotificationLogsTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$NotificationLogsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$NotificationLogsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$NotificationLogsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<int> id = const Value.absent(),
            Value<String> type = const Value.absent(),
            Value<DateTime> triggeredAt = const Value.absent(),
            Value<String?> dateYyyyMmDd = const Value.absent(),
          }) =>
              NotificationLogsCompanion(
            id: id,
            type: type,
            triggeredAt: triggeredAt,
            dateYyyyMmDd: dateYyyyMmDd,
          ),
          createCompanionCallback: ({
            Value<int> id = const Value.absent(),
            required String type,
            required DateTime triggeredAt,
            Value<String?> dateYyyyMmDd = const Value.absent(),
          }) =>
              NotificationLogsCompanion.insert(
            id: id,
            type: type,
            triggeredAt: triggeredAt,
            dateYyyyMmDd: dateYyyyMmDd,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$NotificationLogsTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $NotificationLogsTable,
    NotificationLog,
    $$NotificationLogsTableFilterComposer,
    $$NotificationLogsTableOrderingComposer,
    $$NotificationLogsTableAnnotationComposer,
    $$NotificationLogsTableCreateCompanionBuilder,
    $$NotificationLogsTableUpdateCompanionBuilder,
    (
      NotificationLog,
      BaseReferences<_$AppDatabase, $NotificationLogsTable, NotificationLog>
    ),
    NotificationLog,
    PrefetchHooks Function()>;
typedef $$WfoScheduleHistoryTableCreateCompanionBuilder
    = WfoScheduleHistoryCompanion Function({
  Value<int> id,
  required int wfoDaysMask,
  required DateTime effectiveFrom,
});
typedef $$WfoScheduleHistoryTableUpdateCompanionBuilder
    = WfoScheduleHistoryCompanion Function({
  Value<int> id,
  Value<int> wfoDaysMask,
  Value<DateTime> effectiveFrom,
});

class $$WfoScheduleHistoryTableFilterComposer
    extends Composer<_$AppDatabase, $WfoScheduleHistoryTable> {
  $$WfoScheduleHistoryTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get wfoDaysMask => $composableBuilder(
      column: $table.wfoDaysMask, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get effectiveFrom => $composableBuilder(
      column: $table.effectiveFrom, builder: (column) => ColumnFilters(column));
}

class $$WfoScheduleHistoryTableOrderingComposer
    extends Composer<_$AppDatabase, $WfoScheduleHistoryTable> {
  $$WfoScheduleHistoryTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get wfoDaysMask => $composableBuilder(
      column: $table.wfoDaysMask, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get effectiveFrom => $composableBuilder(
      column: $table.effectiveFrom,
      builder: (column) => ColumnOrderings(column));
}

class $$WfoScheduleHistoryTableAnnotationComposer
    extends Composer<_$AppDatabase, $WfoScheduleHistoryTable> {
  $$WfoScheduleHistoryTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<int> get wfoDaysMask => $composableBuilder(
      column: $table.wfoDaysMask, builder: (column) => column);

  GeneratedColumn<DateTime> get effectiveFrom => $composableBuilder(
      column: $table.effectiveFrom, builder: (column) => column);
}

class $$WfoScheduleHistoryTableTableManager extends RootTableManager<
    _$AppDatabase,
    $WfoScheduleHistoryTable,
    WfoScheduleHistoryEntry,
    $$WfoScheduleHistoryTableFilterComposer,
    $$WfoScheduleHistoryTableOrderingComposer,
    $$WfoScheduleHistoryTableAnnotationComposer,
    $$WfoScheduleHistoryTableCreateCompanionBuilder,
    $$WfoScheduleHistoryTableUpdateCompanionBuilder,
    (
      WfoScheduleHistoryEntry,
      BaseReferences<_$AppDatabase, $WfoScheduleHistoryTable,
          WfoScheduleHistoryEntry>
    ),
    WfoScheduleHistoryEntry,
    PrefetchHooks Function()> {
  $$WfoScheduleHistoryTableTableManager(
      _$AppDatabase db, $WfoScheduleHistoryTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$WfoScheduleHistoryTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$WfoScheduleHistoryTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$WfoScheduleHistoryTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<int> id = const Value.absent(),
            Value<int> wfoDaysMask = const Value.absent(),
            Value<DateTime> effectiveFrom = const Value.absent(),
          }) =>
              WfoScheduleHistoryCompanion(
            id: id,
            wfoDaysMask: wfoDaysMask,
            effectiveFrom: effectiveFrom,
          ),
          createCompanionCallback: ({
            Value<int> id = const Value.absent(),
            required int wfoDaysMask,
            required DateTime effectiveFrom,
          }) =>
              WfoScheduleHistoryCompanion.insert(
            id: id,
            wfoDaysMask: wfoDaysMask,
            effectiveFrom: effectiveFrom,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$WfoScheduleHistoryTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $WfoScheduleHistoryTable,
    WfoScheduleHistoryEntry,
    $$WfoScheduleHistoryTableFilterComposer,
    $$WfoScheduleHistoryTableOrderingComposer,
    $$WfoScheduleHistoryTableAnnotationComposer,
    $$WfoScheduleHistoryTableCreateCompanionBuilder,
    $$WfoScheduleHistoryTableUpdateCompanionBuilder,
    (
      WfoScheduleHistoryEntry,
      BaseReferences<_$AppDatabase, $WfoScheduleHistoryTable,
          WfoScheduleHistoryEntry>
    ),
    WfoScheduleHistoryEntry,
    PrefetchHooks Function()>;

class $AppDatabaseManager {
  final _$AppDatabase _db;
  $AppDatabaseManager(this._db);
  $$OfficeConfigsTableTableManager get officeConfigs =>
      $$OfficeConfigsTableTableManager(_db, _db.officeConfigs);
  $$AttendanceRecordsTableTableManager get attendanceRecords =>
      $$AttendanceRecordsTableTableManager(_db, _db.attendanceRecords);
  $$UserProfilesTableTableManager get userProfiles =>
      $$UserProfilesTableTableManager(_db, _db.userProfiles);
  $$NotificationLogsTableTableManager get notificationLogs =>
      $$NotificationLogsTableTableManager(_db, _db.notificationLogs);
  $$WfoScheduleHistoryTableTableManager get wfoScheduleHistory =>
      $$WfoScheduleHistoryTableTableManager(_db, _db.wfoScheduleHistory);
}
