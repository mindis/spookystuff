/*
Copyright 2007-2010 Selenium committers

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.tribbloid.spookystuff.dsl

import org.openqa.selenium.Capabilities
import org.openqa.selenium.phantomjs.{PhantomJSDriver, PhantomJSDriverService}
import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities}
import org.tribbloid.spookystuff.session.{CleanWebDriver, CleanWebDriverHelper}
import org.tribbloid.spookystuff.{Const, SpookyContext}

//TODO: switch to DriverPool! Tor cannot handle too many connection request.
sealed abstract class DriverFactory extends Serializable{

  final def newInstance(capabilities: Capabilities, spooky: SpookyContext): CleanWebDriver = {
    val result = _newInstance(capabilities, spooky)

    spooky.metrics.driverInitialized += 1
    result
  }

  def _newInstance(capabilities: Capabilities, spooky: SpookyContext): CleanWebDriver
}

case class NaiveDriverFactory(
                          phantomJSPath: String = Const.phantomJSPath,
                          loadImages: Boolean = false,
                          resolution: (Int, Int) = (1920, 1080)
                          )
  extends DriverFactory {

  //  val phantomJSPath: String

  val baseCaps = new DesiredCapabilities
  baseCaps.setJavascriptEnabled(true); //< not really needed: JS enabled by default
  baseCaps.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS, true)
  //  baseCaps.setCapability(CapabilityType.HAS_NATIVE_EVENTS, false)
  baseCaps.setCapability("takesScreenshot", true)
  baseCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomJSPath)
  baseCaps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "loadImages", loadImages)

  //    baseCaps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX+"resourceTimeout", Const.resourceTimeout*1000)

  def newCap(capabilities: Capabilities, spooky: SpookyContext): DesiredCapabilities = {
    val result = new DesiredCapabilities(baseCaps)

    result.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX+"resourceTimeout", spooky.remoteResourceTimeout*1000)

    val userAgent = spooky.userAgent
    if (userAgent != null) result.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", userAgent)

    val proxy = spooky.proxy()

    if (proxy != null)
      result.setCapability(
        PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
        Array("--proxy=" + proxy.addr+":"+proxy.port, "--proxy-type=" + proxy.protocol)
      )

    result.merge(capabilities)
  }

  override def _newInstance(capabilities: Capabilities, spooky: SpookyContext): CleanWebDriver = {

    new PhantomJSDriver(newCap(capabilities, spooky)) with CleanWebDriverHelper
  }
}

////just for debugging
////a bug in this driver has caused it unusable in Firefox 32
//object FirefoxDriverFactory extends DriverFactory {
//
//  val baseCaps = new DesiredCapabilities
//  //  baseCaps.setJavascriptEnabled(true);                //< not really needed: JS enabled by default
//  //  baseCaps.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS,true)
//
//  //  val FirefoxRootPath = "/usr/lib/phantomjs/"
//  //  baseCaps.setCapability("webdriver.firefox.bin", "firefox");
//  //  baseCaps.setCapability("webdriver.firefox.profile", "WebDriver");
//
//  override def newInstance(capabilities: Capabilities, spooky: SpookyContext): WebDriver = {
//    val newCap = baseCaps.merge(capabilities)
//
//    Utils.retry(Const.DFSInPartitionRetry) {
//      Utils.withDeadline(spooky.distributedResourceTimeout) {new FirefoxDriver(newCap)}
//    }
//  }
//}

//object HtmlUnitDriverFactory extends DriverFactory {
//
//  val baseCaps = new DesiredCapabilities
//  baseCaps.setJavascriptEnabled(true);                //< not really needed: JS enabled by default
//  baseCaps.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS,true)
//
//  //  val FirefoxRootPath = "/usr/lib/phantomjs/"
//  //  baseCaps.setCapability("webdriver.firefox.bin", "firefox");
//  //  baseCaps.setCapability("webdriver.firefox.profile", "WebDriver");
//
//  override def newInstance(capabilities: Capabilities, spooky: SpookyContext): WebDriver = {
//    val newCap = baseCaps.merge(capabilities)
//
//    Utils.retry(Const.DFSInPartitionRetry) {
//      Utils.withDeadline(spooky.distributedResourceTimeout) {new HtmlUnitDriver(newCap)}
//    }
//  }
//
//}
