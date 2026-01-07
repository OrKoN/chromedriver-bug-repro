#  Copyright 2025 Google LLC
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging
import pytest
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from PIL import Image

# The chrome and chromedriver installation can take some time.
# Give 5 minutes to install everything.
TIMEOUT = 5 * 60 * 1000


@pytest.fixture(scope="module")
def driver():
    # By default, the test uses the latest stable Chrome version.
    # Replace the "stable" with the specific browser version if needed,
    # e.g. 'canary', '115' or '144.0.7534.0' for example.
    browser_version = "stable"

    options = Options()
    options.add_argument('--window-size=1920,1080')
    options.add_argument('--force-device-scale-factor=2.0')
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.add_argument('--disable-gpu')
    options.add_argument('--disable-dev-shm-usage')
    options.add_argument('--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36')
    options.browser_version = browser_version

    service = Service(service_args=["--log-path=chromedriver.log", "--verbose"])

    driver = webdriver.Chrome(options=options, service=service)
    driver.set_window_size(1920, 1080)

    yield driver

    driver.quit()


@pytest.mark.timeout(TIMEOUT)
def test_should_be_able_to_navigate_to_google_com(driver):
    """This test is intended to verify the setup is correct."""
    driver.get("https://www.google.com")
    logging.info(driver.title)
    assert driver.title == "Google"


@pytest.mark.timeout(TIMEOUT)
def test_issue_reproduction(driver):
    """
    This test reproduces the bug where element.screenshot() does not respect
    the --force-device-scale-factor option.
    1. The browser is launched with a device scale factor of 2.0.
    2. It navigates to https://glints.com/sg.
    3. It takes a screenshot of the whole page and an element.
    4. It asserts that the element screenshot has the expected dimensions,
       which should be scaled by the device scale factor.
    5. The assertion is expected to fail if the bug is present.
    """
    driver.get('https://glints.com/sg')

    element = WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.CLASS_NAME, 'CareersGridComponentssc__Container-sc-zqphsk-0'))
    )

    # This is saved with a resolution of 3840x2160
    driver.save_screenshot('/tmp/screenshot.png')

    # This however, is much smaller - it's the element's size as if the scale ratio is just 1.0.
    element.screenshot('/tmp/element_screenshot.png')

    expected_width = element.size['width'] * 2
    expected_height = element.size['height'] * 2

    from PIL import Image
    with Image.open('/tmp/element_screenshot.png') as img:
        actual_width, actual_height = img.size

    assert actual_width == expected_width
    assert actual_height == expected_height
