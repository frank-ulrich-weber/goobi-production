/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.dataeditor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetsKitodoWriterTest {

    private MetsKitodoWriter metsKitodoWriter = new MetsKitodoWriter();
    private static File manifestFile = new File("./target/classes/META-INF/MANIFEST.MF");

    @BeforeClass
    public static void setUp() throws IOException {

        String manifest =
            "Manifest-Version: 1.0\n" +
            "Archiver-Version: Plexus Archiver\n" +
            "Created-By: Apache Maven\n" +
            "Built-By: tester\n" +
            "Build-Jdk: 1.8.0_144\n" +
            "Specification-Title: Kitodo - Data Editor\n" +
            "Specification-Version: 3.0-SNAPSHOT\n" +
            "Specification-Vendor: kitodo.org\n" +
            "Implementation-Title: Kitodo - Data Editor\n" +
            "Implementation-Version: 3.0-SNAPSHOT\n" +
            "Implementation-Vendor-Id: org.kitodo\n" +
            "Implementation-Vendor: kitodo.org\n" +
            "Implementation-Build-Date: 2018-05-03T08:41:49Z\n";

        FileUtils.write(manifestFile,manifest,"UTF-8");
    }

    @AfterClass
    public static void tearDown() throws IOException {
        Files.deleteIfExists(manifestFile.toPath());
    }

    @Test
    public void shouldWriteMetsFile()
            throws TransformerException, JAXBException, IOException, DatatypeConfigurationException {
        URI xmlFile = URI.create("./src/test/resources/testmeta.xml");

        URI xmlTestFile = Paths.get(System.getProperty("user.dir") + "/target/test-classes/newtestmeta.xml").toUri();

        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(xmlFile);
        metsKitodoWriter.save(metsKitodoWrapper.getMets(), xmlTestFile);
        MetsKitodoWrapper savedMetsKitodoWrapper = new MetsKitodoWrapper(xmlTestFile);
        Files.deleteIfExists(Paths.get(xmlTestFile));

        String loadedMetadata = metsKitodoWrapper.getKitodoTypeByMdSecId("DMDLOG_0000").getMetadata().get(0).getValue();
        String savedMetadata = savedMetsKitodoWrapper.getKitodoTypeByMdSecId("DMDLOG_0000").getMetadata().get(0)
                .getValue();

        Assert.assertEquals("The metadata of the loaded and the saved mets file are not equal", loadedMetadata,
            savedMetadata);
        Assert.assertEquals("The number of dmdSec elements of the loaded and the saved mets file are not equal",
            metsKitodoWrapper.getDmdSecs().size(), savedMetsKitodoWrapper.getDmdSecs().size());
    }

    @Test
    public void shouldWriteMetsFileFromOldFormat()
            throws TransformerException, JAXBException, IOException, DatatypeConfigurationException {
        URI xmlFile = URI.create("./src/test/resources/testmetaOldFormat.xml");

        URI xmlTestFile = Paths.get(System.getProperty("user.dir") + "/target/test-classes/newtestmetaold.xml").toUri();

        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(xmlFile);
        metsKitodoWriter.save(metsKitodoWrapper.getMets(), xmlTestFile);
        MetsKitodoWrapper savedMetsKitodoWrapper = new MetsKitodoWrapper(xmlTestFile);
        Files.deleteIfExists(Paths.get(xmlTestFile));

        String loadedMetadata = metsKitodoWrapper.getKitodoTypeByMdSecId("DMDLOG_0000").getMetadata().get(0).getValue();
        String savedMetadata = savedMetsKitodoWrapper.getKitodoTypeByMdSecId("DMDLOG_0000").getMetadata().get(0)
                .getValue();

        Assert.assertEquals("The metadata of the loaded and the saved mets file are not equal", loadedMetadata,
            savedMetadata);
        Assert.assertEquals("The number of dmdSec elements of the loaded and the saved mets file are not equal",
            metsKitodoWrapper.getDmdSecs().size(), savedMetsKitodoWrapper.getDmdSecs().size());

        Assert.assertEquals("Conversation note was not inserted to mets header", 2,
            metsKitodoWrapper.getMets().getMetsHdr().getAgent().get(0).getNote().size());
    }
}
