/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4;

import com.kondra.kos.zero4.brandset.Brandset;
import com.tccc.kos.commons.core.context.annotations.Autowired;
import com.tccc.kos.commons.core.vfs.VFSSource;
import com.tccc.kos.commons.kab.KabFile;
import com.tccc.kos.commons.util.KosUtil;
import com.tccc.kos.commons.util.resource.ClassLoaderResourceLoader;
import com.tccc.kos.core.service.app.BaseAppConfig;
import com.tccc.kos.core.service.app.SystemApplication;
import com.tccc.kos.core.service.browser.BrowserService;
import com.tccc.kos.core.service.region.XmlRegionFactory;
import com.tccc.kos.ext.dispense.service.ingredient.IngredientService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * System application for Zero4 demo kit dispenser.
 * <p>
 * When kOS boots and finishes initializing all internal services, it will
 * load a single user-defined system application which takes control of
 * the device and defines how it works. This particular system application
 * turns the Zero4 board into a mini-dispenser, utilizing the four micro,
 * and two macros pumps to pour recipe based beverages.
 * <p>
 * While this demo is for a dispenser, it illustrates a number of generic
 * kOS concepts that commonly apply to any type of device.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
@Slf4j
public class Zero4App extends SystemApplication<BaseAppConfig> {
    // The KAB type for the user interface kab
    private static final String UI_KAB_TYPE = "zero4.ui";

    @Autowired
    private IngredientService ingredientService;   // used to register ingredients from the brandset
    @Autowired
    private BrowserService browserService;         // used to navigate the browser to our ui
    @Getter
    private Brandset brandset;                     // our brandset loaded from another KAB
    private VFSSource uiVfsSource;                 // where the ui KAB is mounted in vfs

    /**
     * Called when the application loads. Any beans that the application wants to create
     * that need to be autowired / configured, should be created here and added to the context.
     * When this method returns, all the beans in the context will be autowired and configured,
     * making them available for use in the {@code start()} method.
     */
    @Override
    public void load() {
        // add test controller so we can enable / disable pumps
        addToCtx(new TestController());
    }

    /**
     * Called when the application is started. If this application had any
     * services, controllers or other beans to add to the application {@code BeanContext}
     * then we would create these in {@code load()}. This would allow kOS to autowire,
     * initialize and configure the beans before {@code start()} is called.
     */
    @Override
    public void start() throws Exception {
        // Many devices have settings or functions that depend on the geographic region
        // device is located in, such as default units, time/date formats, rfid frequencies,
        // regulatory information and so on. kOS provides {@code RegionService} to centrally
        // manage this type of data. Region data is typically provided by an extensible XML file.
        // The following code creates a factory to load region data from regions.xml.
        XmlRegionFactory factory = new XmlRegionFactory();
        factory.addLoader(new ClassLoaderResourceLoader(getClass().getClassLoader()));
        factory.load("regions.xml");

        // Install the region data into {@code RegionService}. At this point the region can be
        // changed using {@code RegionService} api's or even standard config api's.
        installRegions(factory.getRegions());

        // This application defines a custom brandset which contains a list of possible
        // ingredients and recipe-based beverages. This data is stored in a separate KAB
        // file which allows it to be easily swapped. The following code searches for the
        // brandset in the same manifest section that the system application was in.
        KabFile kab = getSection().getKabByType("zero4.brandset");
        if (kab != null) {
            // Load the brandset json from the KAB into a brandset object
            brandset = KosUtil.getMapper().readValue(kab.getInputStream("brandset.json"), Brandset.class);

            // The brandset includes a collection of possible ingredients which we want to
            // make available to various kOS services. This is done by adding the brandset
            // as an {@code IngredientSource}.
            ingredientService.setDefaultSource(brandset);

            // The brandset KAB also contains additional data and content that will be used
            // by the web UI. By mounting the KAB into the kOS VFS (virtual file system), the
            // content in the KAB becomes accessible via the internal kOS web server.
            VFSSource source = getVfs().mount("/brandset", kab);

            // Log that we mounted the KAB and where it's located in VFS
            log.info("Brandset mounted at: {}", source.getBasePath());
        }

        // If there is a user interface KAB in our section, mount it into the vfs so that
        // we can navigate the browser to this user interface when we finish loading.
        kab = getSection().getKabByType(UI_KAB_TYPE);
        if (kab != null) {
            uiVfsSource = getVfs().mount("/ui", kab);
        }

        // kOS models hardware in the device in a container called an assembly. This allows the
        // logical version of hardware to be created and configured before real hardware can
        // connect to it, preventing race conditions. We create the {@code Assembly} and install
        // it, making it available in kOS.
        installAssembly(new Zero4Assembly());
    }

    /**
     * Called when the application is fully started and ready for use. While {@code start()} is
     * responsible for installing hardware, configuring VFS and other initialization steps, kOS
     * will prevent any endpoint or VFS access to the application until {@code start()} returns.
     * Once all application services are fully available, {@code started()} is called to notify
     * the application that it is fully available and running.
     */
    @Override
    public void started() throws Exception {
        // nav to the ui
        if (uiVfsSource != null) {
            browserService.goToUrl(uiVfsSource.getFullPath("index.html"));
        }
    }
}
