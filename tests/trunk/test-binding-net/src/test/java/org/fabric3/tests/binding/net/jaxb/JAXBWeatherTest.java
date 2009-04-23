/*
 * Fabric3
 * Copyright � 2008 Metaform Systems Limited
 *
 * This proprietary software may be used only connection with the Fabric3 license
 * (the �License�), a copy of which is included in the software or may be
 * obtained at: http://www.metaformsystems.com/licenses/license.html.

 * Software distributed under the License is distributed on an �as is� basis,
 * without warranties or conditions of any kind.  See the License for the
 * specific language governing permissions and limitations of use of the software.
 * This software is distributed in conjunction with other software licensed under
 * different terms.  See the separate licenses for those programs included in the
 * distribution for the permitted and restricted uses of such software.
 *
 */
package org.fabric3.tests.binding.net.jaxb;

import java.util.Date;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.oasisopen.sca.annotation.Reference;

/**
 * @version $Revision$ $Date$
 */
public class JAXBWeatherTest extends TestCase {
    @Reference
    protected WeatherService weatherService;

    public void testWeather() {

        WeatherRequest weatherRequest = new WeatherRequest();
        weatherRequest.setCity("London");
        weatherRequest.setDate(new Date());

        WeatherResponse weatherResponse = weatherService.getWeather(weatherRequest);

        Assert.assertEquals(WeatherCondition.SUNNY, weatherResponse.getCondition());
        assertEquals(25.0, weatherResponse.getTemperatureMinimum());
        assertEquals(40.0, weatherResponse.getTemperatureMaximum());

    }

}