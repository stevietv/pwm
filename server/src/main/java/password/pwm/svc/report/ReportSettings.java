/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2021 The PWM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package password.pwm.svc.report;

import lombok.Builder;
import lombok.Value;
import password.pwm.AppProperty;
import password.pwm.bean.DomainID;
import password.pwm.config.AppConfig;
import password.pwm.config.DomainConfig;
import password.pwm.config.PwmSetting;
import password.pwm.config.value.data.UserPermission;
import password.pwm.error.PwmUnrecoverableException;
import password.pwm.util.json.JsonFactory;
import password.pwm.util.java.TimeDuration;
import password.pwm.util.logging.PwmLogger;
import password.pwm.util.secure.PwmHashAlgorithm;
import password.pwm.util.secure.SecureEngine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Value
@Builder
class ReportSettings implements Serializable
{
    private static final PwmLogger LOGGER = PwmLogger.forClass( ReportSettings.class );

    private boolean dailyJobEnabled;

    @Builder.Default
    private TimeDuration maxCacheAge = TimeDuration.of( 10, TimeDuration.Unit.DAYS );

    @Builder.Default
    // sorted so json output -> hash is consistent.
    private SortedMap<DomainID, List<UserPermission>> searchFilter = Collections.emptySortedMap();

    @Builder.Default
    private int jobOffsetSeconds = 0;

    @Builder.Default
    private int maxSearchSize = 100 * 1000;

    @Builder.Default
    private TimeDuration searchTimeout = TimeDuration.SECONDS_30;

    @Builder.Default
    private List<Integer> trackDays = Collections.emptyList();

    @Builder.Default
    private int reportJobThreads = 1;

    @Builder.Default
    private JobIntensity reportJobIntensity = JobIntensity.LOW;

    public enum JobIntensity
    {
        LOW,
        MEDIUM,
        HIGH,
    }

    static ReportSettings readSettingsFromConfig( final AppConfig config )
    {
        final SortedMap<DomainID, List<UserPermission>> searchFilters = Collections.unmodifiableSortedMap( new TreeMap<>( config.getDomainConfigs().values().stream()
                .collect( Collectors.toMap(
                        DomainConfig::getDomainID,
                        domainConfig -> domainConfig.readSettingAsUserPermission( PwmSetting.REPORTING_USER_MATCH ) ) ) ) );

        final ReportSettings.ReportSettingsBuilder builder = ReportSettings.builder();
        builder.maxCacheAge( TimeDuration.of( Long.parseLong( config.readAppProperty( AppProperty.REPORTING_MAX_REPORT_AGE_SECONDS ) ), TimeDuration.Unit.SECONDS ) );
        builder.searchFilter( searchFilters );
        builder.maxSearchSize ( ( int ) config.readSettingAsLong( PwmSetting.REPORTING_MAX_QUERY_SIZE ) );
        builder.dailyJobEnabled( config.readSettingAsBoolean( PwmSetting.REPORTING_ENABLE_DAILY_JOB ) );
        builder.searchTimeout( TimeDuration.of( Long.parseLong( config.readAppProperty( AppProperty.REPORTING_LDAP_SEARCH_TIMEOUT_MS ) ), TimeDuration.Unit.MILLISECONDS ) );


        {
            int reportJobOffset = ( int ) config.readSettingAsLong( PwmSetting.REPORTING_JOB_TIME_OFFSET );
            if ( reportJobOffset > 60 * 60 * 24 )
            {
                reportJobOffset = 0;
            }

            builder.jobOffsetSeconds( reportJobOffset );
        }

        builder.trackDays( parseDayIntervalStr( config ) );

        builder.reportJobThreads( Integer.parseInt( config.readAppProperty( AppProperty.REPORTING_LDAP_SEARCH_THREADS ) ) );

        builder.reportJobIntensity( config.readSettingAsEnum( PwmSetting.REPORTING_JOB_INTENSITY, JobIntensity.class ) );

        return builder.build();
    }

    private static List<Integer> parseDayIntervalStr( final AppConfig domainConfig )
    {
        final List<String> configuredValues = new ArrayList<>();
        if ( domainConfig != null )
        {
            configuredValues.addAll( domainConfig.readSettingAsStringArray( PwmSetting.REPORTING_SUMMARY_DAY_VALUES ) );
        }
        if ( configuredValues.isEmpty() )
        {
            configuredValues.add( "1" );
        }
        final List<Integer> returnValue = new ArrayList<>();
        for ( final String splitDay : configuredValues )
        {
            try
            {
                final int dayValue = Integer.parseInt( splitDay );
                returnValue.add( dayValue );
            }
            catch ( final NumberFormatException e )
            {
                LOGGER.error( () -> "error parsing reporting summary day value '" + splitDay + "', error: " + e.getMessage() );
            }
        }
        Collections.sort( returnValue );
        return Collections.unmodifiableList( returnValue );
    }

    String getSettingsHash( )
            throws PwmUnrecoverableException
    {
        return SecureEngine.hash( JsonFactory.get().serialize( this ), PwmHashAlgorithm.SHA512 );
    }
}
