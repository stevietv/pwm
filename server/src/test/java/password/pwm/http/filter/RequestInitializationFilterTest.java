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

package password.pwm.http.filter;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import password.pwm.bean.DomainID;
import password.pwm.config.AppConfig;
import password.pwm.config.PwmSetting;
import password.pwm.config.stored.StoredConfigKey;
import password.pwm.config.stored.StoredConfigurationFactory;
import password.pwm.config.stored.StoredConfigurationModifier;
import password.pwm.config.value.BooleanValue;
import password.pwm.error.PwmUnrecoverableException;
import password.pwm.http.HttpHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class RequestInitializationFilterTest
{
    @Test
    public void readUserNetworkAddressTest()
            throws PwmUnrecoverableException
    {
        final AppConfig conf = new AppConfig( StoredConfigurationFactory.newConfig() );
        final HttpServletRequest mockRequest = Mockito.mock( HttpServletRequest.class );
        Mockito.when( mockRequest.getRemoteAddr() ).thenReturn( "10.1.1.1" );

        final String resultIP = RequestInitializationFilter.readUserNetworkAddress( mockRequest, conf ).orElseThrow();
        Assert.assertEquals( "10.1.1.1", resultIP );
    }

    @Test
    public void readUserNetworkAddressTestBogus()
            throws PwmUnrecoverableException
    {
        final AppConfig conf = new AppConfig( StoredConfigurationFactory.newConfig() );
        final HttpServletRequest mockRequest = Mockito.mock( HttpServletRequest.class );
        Mockito.when( mockRequest.getRemoteAddr() ).thenReturn( "10.1.1.1m" );

        final Optional<String> resultIP = RequestInitializationFilter.readUserNetworkAddress( mockRequest, conf );
        Assert.assertTrue( resultIP.isEmpty() );
    }

    @Test
    public void readUserNetworkAddressTestXForward()
            throws PwmUnrecoverableException
    {
        final AppConfig conf = new AppConfig( StoredConfigurationFactory.newConfig() );
        final HttpServletRequest mockRequest = Mockito.mock( HttpServletRequest.class );
        Mockito.when( mockRequest.getRemoteAddr() ).thenReturn( "10.1.1.1" );
        Mockito.when( mockRequest.getHeader( HttpHeader.XForwardedFor.getHttpName() ) ).thenReturn( "10.1.1.2" );

        final String resultIP = RequestInitializationFilter.readUserNetworkAddress( mockRequest, conf ).orElseThrow();
        Assert.assertEquals( "10.1.1.2", resultIP );
    }

    @Test
    public void readUserNetworkAddressTestBogusXForward()
            throws PwmUnrecoverableException
    {
        final AppConfig conf = new AppConfig( StoredConfigurationFactory.newConfig() );
        final HttpServletRequest mockRequest = Mockito.mock( HttpServletRequest.class );
        Mockito.when( mockRequest.getRemoteAddr() ).thenReturn( "10.1.1.1" );
        Mockito.when( mockRequest.getHeader( HttpHeader.XForwardedFor.getHttpName() ) ).thenReturn( "10.1.1.2a" );

        final String resultIP = RequestInitializationFilter.readUserNetworkAddress( mockRequest, conf ).orElseThrow();
        Assert.assertEquals( "10.1.1.1", resultIP );
    }

    @Test
    public void readUserNetworkAddressTestMultipleXForward()
            throws PwmUnrecoverableException
    {
        final AppConfig conf = new AppConfig( StoredConfigurationFactory.newConfig() );
        final HttpServletRequest mockRequest = Mockito.mock( HttpServletRequest.class );
        Mockito.when( mockRequest.getRemoteAddr() ).thenReturn( "10.1.1.1" );
        Mockito.when( mockRequest.getHeader( HttpHeader.XForwardedFor.getHttpName() ) ).thenReturn( "10.1.1.2, 10.1.1.3" );

        final String resultIP = RequestInitializationFilter.readUserNetworkAddress( mockRequest, conf ).orElseThrow();
        Assert.assertEquals( "10.1.1.2", resultIP );
    }

    @Test
    public void readUserNetworkAddressTestMultipleBogusXForward()
            throws PwmUnrecoverableException
    {
        final AppConfig conf = new AppConfig( StoredConfigurationFactory.newConfig() );
        final HttpServletRequest mockRequest = Mockito.mock( HttpServletRequest.class );
        Mockito.when( mockRequest.getRemoteAddr() ).thenReturn( "10.1.1.1" );
        Mockito.when( mockRequest.getHeader( HttpHeader.XForwardedFor.getHttpName() ) ).thenReturn( "10.1.1.2a, 10.1.1.3" );

        final String resultIP = RequestInitializationFilter.readUserNetworkAddress( mockRequest, conf ).orElseThrow();
        Assert.assertEquals( "10.1.1.3", resultIP );
    }

    @Test
    public void readUserNetworkAddressTestIPv6()
            throws PwmUnrecoverableException
    {
        final AppConfig conf = new AppConfig( StoredConfigurationFactory.newConfig() );
        final HttpServletRequest mockRequest = Mockito.mock( HttpServletRequest.class );
        Mockito.when( mockRequest.getRemoteAddr() ).thenReturn( "10.1.1.1" );
        Mockito.when( mockRequest.getHeader( HttpHeader.XForwardedFor.getHttpName() ) ).thenReturn( "10.1.1.2a, 2001:0db8:85a3:0000:0000:8a2e:0370:7334" );

        final String resultIP = RequestInitializationFilter.readUserNetworkAddress( mockRequest, conf ).orElseThrow();
        Assert.assertEquals( "2001:0db8:85a3:0000:0000:8a2e:0370:7334", resultIP );
    }

    @Test
    public void readUserNetworkAddressTestDisabledXForwardedFor()
            throws PwmUnrecoverableException
    {
        final StoredConfigurationModifier modifier = StoredConfigurationFactory.newModifiableConfig();
        final StoredConfigKey key = StoredConfigKey.forSetting( PwmSetting.USE_X_FORWARDED_FOR_HEADER, null, DomainID.systemId() );
        modifier.writeSetting( key, BooleanValue.of( false ), null );
        final AppConfig conf = new AppConfig( modifier.newStoredConfiguration() );
        final HttpServletRequest mockRequest = Mockito.mock( HttpServletRequest.class );
        Mockito.when( mockRequest.getRemoteAddr() ).thenReturn( "10.1.1.1" );
        Mockito.when( mockRequest.getHeader( HttpHeader.XForwardedFor.getHttpName() ) ).thenReturn( "10.1.1.2" );

        final String resultIP = RequestInitializationFilter.readUserNetworkAddress( mockRequest, conf ).orElseThrow();
        Assert.assertEquals( "10.1.1.1", resultIP );
    }
}
