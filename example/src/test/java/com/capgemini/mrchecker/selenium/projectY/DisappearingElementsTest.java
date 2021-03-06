package com.capgemini.mrchecker.selenium.projectY;

import static org.junit.Assert.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.capgemini.mrchecker.core.groupTestCases.testSuites.tags.TestsChrome;
import com.capgemini.mrchecker.core.groupTestCases.testSuites.tags.TestsFirefox;
import com.capgemini.mrchecker.core.groupTestCases.testSuites.tags.TestsIE;
import com.capgemini.mrchecker.core.groupTestCases.testSuites.tags.TestsSelenium;
import com.capgemini.mrchecker.selenium.pages.projectY.DisappearingElementsPage;

@TestsSelenium
@TestsChrome
@TestsFirefox
@TestsIE
public class DisappearingElementsTest extends TheInternetBaseTest {
	
	private static final int				totalNumberOfMenuButtons	= 5;
	private static DisappearingElementsPage	disappearingElementsPage;
	private static int						numberOfMenuButtons			= 0;
	
	@BeforeAll
	public static void setUpBeforeClass() {
		disappearingElementsPage = shouldTheInternetPageBeOpened().clickDisappearingElementsLink();
		
		logStep("Verify if Disappearing Elements page is opened");
		assertTrue("Unable to open Disappearing Elements page", disappearingElementsPage.isLoaded());
		
		logStep("Verify if menu button elements are visible");
		numberOfMenuButtons = disappearingElementsPage.getNumberOfMenuButtons();
		assertTrue("Unable to display menu", numberOfMenuButtons > 0);
	}
	
	@Test
	public void shouldMenuButtonElementAppearAndDisappearAfterRefreshTest() {
		logStep("Click refresh button until menu button appears");
		disappearingElementsPage.refreshPageUntilWebElementAppears(true);
		
		logStep("Verify if menu button element appeared");
		assertNotNull("Unable to disappear menu button element", disappearingElementsPage.getGalleryMenuElement());
		assertEquals("The number of button elements after refresh is incorrect", totalNumberOfMenuButtons, disappearingElementsPage.getNumberOfMenuButtons());
		
		logStep("Click refresh button until menu button disappears");
		disappearingElementsPage.refreshPageUntilWebElementAppears(false);
		
		logStep("Verify if menu button element disappeared");
		assertNull("Unable to appear menu button element", disappearingElementsPage.getGalleryMenuElement());
		assertTrue("The number of button elements after refresh is incorrect", totalNumberOfMenuButtons > disappearingElementsPage.getNumberOfMenuButtons());
	}
	
}
