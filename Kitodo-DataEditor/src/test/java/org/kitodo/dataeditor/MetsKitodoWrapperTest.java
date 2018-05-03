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
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kitodo.dataformat.metskitodo.KitodoType;
import org.kitodo.dataformat.metskitodo.MdSecType;
import org.kitodo.dataformat.metskitodo.MetadataGroupType;
import org.kitodo.dataformat.metskitodo.MetadataType;
import org.kitodo.dataformat.metskitodo.ObjectFactory;
import org.kitodo.dataformat.metskitodo.StructLinkType;

public class MetsKitodoWrapperTest {

    private URI xmlfile = URI.create("./src/test/resources/testmeta.xml");
    private ObjectFactory objectFactory = new ObjectFactory();
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
    public void shouldAddSmLink() throws DatatypeConfigurationException, IOException {
        String from = "from test";
        String to = "to test";

        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper();
        metsKitodoWrapper.addSmLink(from, to);

        StructLinkType.SmLink smLink = (StructLinkType.SmLink) metsKitodoWrapper.getMets().getStructLink()
                .getSmLinkOrSmLinkGrp().get(0);

        Assert.assertEquals("'from' attribute of smLink was wrong", from, smLink.getFrom());
        Assert.assertEquals("'to' attribute of smLink was wrong", to, smLink.getTo());
    }

    @Test
    public void shouldAddMetsHeader() throws DatatypeConfigurationException, IOException {

        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper();
        String role = metsKitodoWrapper.getMets().getMetsHdr().getAgent().get(0).getROLE();
        String name = metsKitodoWrapper.getMets().getMetsHdr().getAgent().get(0).getName();
        String type = metsKitodoWrapper.getMets().getMetsHdr().getAgent().get(0).getTYPE();
        String otherType = metsKitodoWrapper.getMets().getMetsHdr().getAgent().get(0).getOTHERTYPE();

        Assert.assertEquals("Role of mets header agent was inserted wrong", "CREATOR", role);
        Assert.assertEquals("Type of mets header agent was inserted wrong", "OTHER", type);
        Assert.assertEquals("OtherType of mets header agent was inserted wrong", "SOFTWARE", otherType);
        Assert.assertTrue("Name of mets header agent was inserted wrong", name.contains("Kitodo"));
    }

    @Test
    public void shouldCreateMetsByFile() throws JAXBException, TransformerException, IOException, DatatypeConfigurationException {
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(xmlfile);
        Assert.assertEquals("Number of dmdSec elements was wrong!", 3, metsKitodoWrapper.getDmdSecs().size());
    }

    @Test
    public void shouldReadValues() throws JAXBException, TransformerException, IOException, DatatypeConfigurationException {
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(xmlfile);
        String id = metsKitodoWrapper.getMets().getDmdSec().get(0).getID();
        Assert.assertEquals("Reading id of dmdSec data out of mets was not correct", "DMDLOG_0000", id);
    }

    @Test
    public void shouldReadKitodoMetadata() throws JAXBException, TransformerException, IOException, DatatypeConfigurationException {
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(xmlfile);
        KitodoType kitodoType = metsKitodoWrapper.getKitodoTypeByMdSecIndex(0);

        MetadataType metadataType = kitodoType.getMetadata().get(1);
        Assert.assertEquals("Reading data of type 'name' out of kitodo format was not correct", "PublisherName",
            metadataType.getName());
        Assert.assertEquals("Reading value out of kitodo metadata format was not correct", "Test Publisher",
            metadataType.getValue());
    }

    @Test
    public void shouldReadKitodoMetadataGroup() throws JAXBException, TransformerException, IOException, DatatypeConfigurationException {
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(xmlfile);
        KitodoType kitodoType = metsKitodoWrapper.getKitodoTypeByMdSecIndex(0);

        MetadataGroupType metadataGroupType = kitodoType.getMetadataGroup().get(0).getMetadataGroup().get(0);
        Assert.assertEquals("Reading data of type 'name' out of kitodo format was not correct", "subTypIdentifierPPN",
            metadataGroupType.getMetadata().get(1).getName());
        Assert.assertEquals("Reading value out of kitodo metadata was not correct", "sub10457187X",
            metadataGroupType.getMetadata().get(1).getValue());
    }

    @Test
    public void shouldReadGoobiMetadata() throws JAXBException, TransformerException, IOException, DatatypeConfigurationException {
        URI oldXmlfile = URI.create("./src/test/resources/testmetaOldFormat.xml");
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(oldXmlfile);
        KitodoType kitodoType = metsKitodoWrapper.getKitodoTypeByMdSecIndex(0);

        MetadataType metadataType = kitodoType.getMetadata().get(1);
        Assert.assertEquals("Reading data of type 'name' out of kitodo format was not correct", "PublisherName",
            metadataType.getName());
        Assert.assertEquals("Reading value out of kitodo metadata was not correct", "Test Publisher",
            metadataType.getValue());
    }

    @Test
    public void shouldReadGoobiMetadataGroup() throws JAXBException, TransformerException, IOException, DatatypeConfigurationException {
        URI oldXmlfile = URI.create("./src/test/resources/testmetaOldFormat.xml");
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(oldXmlfile);
        KitodoType kitodoType = metsKitodoWrapper.getKitodoTypeByMdSecIndex(0);

        MetadataGroupType metadataGroupType = kitodoType.getMetadataGroup().get(0);
        Assert.assertEquals("Reading data of type 'name' out of kitodo format was not correct", "typIdentifierPPN",
            metadataGroupType.getMetadata().get(1).getName());
        Assert.assertEquals("Reading value out of kitodo metadata was not correct", "10457187X",
            metadataGroupType.getMetadata().get(1).getValue());
    }

    @Test
    public void shouldReadKitodoMetadataById()
        throws JAXBException, TransformerException, IOException, DatatypeConfigurationException {
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(xmlfile);
        KitodoType kitodoType = metsKitodoWrapper.getKitodoTypeByMdSecId("DMDLOG_0002");

        MetadataType metadataType = kitodoType.getMetadata().get(0);
        Assert.assertEquals("Reading data of type 'name' out of kitodo format was not correct", "TitleDocMain",
            metadataType.getName());
        Assert.assertEquals("Reading value out of kitodo metadata was not correct", "[Seite 157r-181v]",
            metadataType.getValue());
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldNotReadKitodoMetadataByNotExistingId()
        throws JAXBException, TransformerException, IOException, DatatypeConfigurationException {
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper(xmlfile);
        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage("MdSec element with id: not existing was not found");
        metsKitodoWrapper.getKitodoTypeByMdSecId("not existing");
    }

    @Test
    public void shouldNotReadNotExistingMdSecByIndex() throws DatatypeConfigurationException, IOException {
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper();
        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage("MdSec element with index: 0 does not exist");
        metsKitodoWrapper.getKitodoTypeByMdSecIndex(0);
    }

    @Test
    public void shouldNotReadNotExistingKitodoMetadataByIndex() throws DatatypeConfigurationException, IOException {
        MetsKitodoWrapper metsKitodoWrapper = new MetsKitodoWrapper();
        MdSecType mdSecType = objectFactory.createMdSecType();
        MdSecType.MdWrap mdSecTypeMdWrap = objectFactory.createMdSecTypeMdWrap();
        mdSecTypeMdWrap.setXmlData(objectFactory.createMdSecTypeMdWrapXmlData());
        mdSecType.setMdWrap(mdSecTypeMdWrap);
        metsKitodoWrapper.getMets().getDmdSec().add(mdSecType);
        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage("MdSec element with index: 0 does not have kitodo metadata");
        metsKitodoWrapper.getKitodoTypeByMdSecIndex(0);
    }
}
