package com.siemens.cto.aem.service.jvm.impl;

import com.siemens.cto.aem.common.domain.model.group.Group;
import com.siemens.cto.aem.common.domain.model.id.Identifier;
import com.siemens.cto.aem.common.domain.model.jvm.Jvm;
import com.siemens.cto.aem.common.domain.model.jvm.JvmState;
import com.siemens.cto.aem.common.domain.model.path.Path;
import com.siemens.cto.aem.common.domain.model.user.User;
import com.siemens.cto.aem.common.exception.BadRequestException;
import com.siemens.cto.aem.common.properties.ApplicationProperties;
import com.siemens.cto.aem.common.request.group.AddJvmToGroupRequest;
import com.siemens.cto.aem.common.request.jvm.CreateJvmAndAddToGroupsRequest;
import com.siemens.cto.aem.common.request.jvm.CreateJvmRequest;
import com.siemens.cto.aem.common.request.jvm.UpdateJvmRequest;
import com.siemens.cto.aem.persistence.service.JvmPersistenceService;
import com.siemens.cto.aem.service.VerificationBehaviorSupport;
import com.siemens.cto.aem.service.group.GroupService;
import com.siemens.cto.aem.service.spring.component.GrpStateComputationAndNotificationSvc;
import com.siemens.cto.aem.service.state.StateNotificationService;
import com.siemens.cto.aem.service.webserver.component.ClientFactoryHelper;
import com.siemens.cto.toc.files.FileManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JvmServiceImplVerifyTest extends VerificationBehaviorSupport {


    private JvmPersistenceService jvmPersistenceService = mock(JvmPersistenceService.class);;
    private GroupService groupService = mock(GroupService.class);
    private User user;
    private FileManager fileManager = mock(FileManager.class);
    private StateNotificationService stateNotificationService = mock(StateNotificationService.class);
    private GrpStateComputationAndNotificationSvc grpStateComputationAndNotificationSvc = mock(GrpStateComputationAndNotificationSvc.class);


    @Mock
    private ClientFactoryHelper mockClientFactoryHelper;

    @InjectMocks
    private JvmServiceImpl impl = new JvmServiceImpl(jvmPersistenceService, groupService, fileManager, stateNotificationService, grpStateComputationAndNotificationSvc);
    ;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        user = new User("unused");
//        impl = new JvmServiceImpl(jvmPersistenceService, groupService, fileManager, stateNotificationService, grpStateComputationAndNotificationSvc);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateValidate() {
        System.setProperty(ApplicationProperties.PROPERTIES_ROOT_PATH, "./src/test/resources");

        final CreateJvmRequest createJvmRequest = mock(CreateJvmRequest.class);
        final Jvm jvm = new Jvm(new Identifier<Jvm>(99L), "testJvm", new HashSet<Group>());
        when(jvmPersistenceService.createJvm(any(CreateJvmRequest.class))).thenReturn(jvm);

        impl.createJvm(createJvmRequest, user);

        verify(createJvmRequest, times(1)).validate();
        verify(jvmPersistenceService, times(1)).createJvm(createJvmRequest);

        System.clearProperty(ApplicationProperties.PROPERTIES_ROOT_PATH);
    }

    @Test
    public void testCreateValidateAdd() {

        final CreateJvmRequest createJvmRequest = mock(CreateJvmRequest.class);
        final CreateJvmAndAddToGroupsRequest command = mock(CreateJvmAndAddToGroupsRequest.class);
        final Jvm jvm = mockJvmWithId(new Identifier<Jvm>(-123456L));
        final Set<AddJvmToGroupRequest> addCommands = createMockedAddRequests(3);

        when(command.toAddRequestsFor(eq(jvm.getId()))).thenReturn(addCommands);
        when(command.getCreateCommand()).thenReturn(createJvmRequest);
        when(jvmPersistenceService.createJvm(createJvmRequest)).thenReturn(jvm);

        impl.createAndAssignJvm(command,
                user);

        verify(createJvmRequest, times(1)).validate();
        verify(jvmPersistenceService, times(1)).createJvm(createJvmRequest);
        for (final AddJvmToGroupRequest addCommand : addCommands) {
            verify(groupService, times(1)).addJvmToGroup(matchCommand(addCommand),
                    eq(user));
        }
    }

    @Test
    public void testUpdateJvmShouldValidateCommand() {

        final UpdateJvmRequest updateJvmRequest = mock(UpdateJvmRequest.class);
        final Set<AddJvmToGroupRequest> addCommands = createMockedAddRequests(5);

        when(updateJvmRequest.getAssignmentCommands()).thenReturn(addCommands);

        impl.updateJvm(updateJvmRequest,
                user);

        verify(updateJvmRequest, times(1)).validate();
        verify(jvmPersistenceService, times(1)).updateJvm(updateJvmRequest);
        verify(jvmPersistenceService, times(1)).removeJvmFromGroups(Matchers.<Identifier<Jvm>>anyObject());
        for (final AddJvmToGroupRequest addCommand : addCommands) {
            verify(groupService, times(1)).addJvmToGroup(matchCommand(addCommand),
                    eq(user));
        }
    }

    @Test
    public void testRemoveJvm() {

        final Identifier<Jvm> id = new Identifier<>(-123456L);

        impl.removeJvm(id);

        verify(jvmPersistenceService, times(1)).removeJvm(eq(id));
    }

    @Test
    public void testFindByName() {

        final String fragment = "unused";

        impl.findJvms(fragment);

        verify(jvmPersistenceService, times(1)).findJvms(eq(fragment));
    }

    @Test(expected = BadRequestException.class)
    public void testFindByInvalidName() {

        final String badFragment = "";

        impl.findJvms(badFragment);
    }

    @Test
    public void testFindByGroup() {

        final Identifier<Group> id = new Identifier<>(-123456L);

        impl.findJvms(id);

        verify(jvmPersistenceService, times(1)).findJvmsBelongingTo(eq(id));
    }

    @Test
    public void testGetAll() {

        impl.getJvms();

        verify(jvmPersistenceService, times(1)).getJvms();
    }


    @Test
    public void testGenerateConfig() throws IOException {

        final Jvm jvm = new Jvm(new Identifier<Jvm>(-123456L),
                "jvm-name", "host-name", new HashSet<Group>(), 80, 443, 443, 8005, 8009, new Path("/"),
                "EXAMPLE_OPTS=%someEnv%/someVal", JvmState.JVM_STOPPED, null);
        final ArrayList<Jvm> jvms = new ArrayList<>(1);
        jvms.add(jvm);

        when(jvmPersistenceService.findJvms(eq(jvm.getJvmName()))).thenReturn(jvms);
        when(jvmPersistenceService.getJvmTemplate(eq("server.xml"), eq(jvm.getId()))).thenReturn("<server>test</server>");
        String generatedXml = impl.generateConfigFile(jvm.getJvmName(), "server.xml");

        assert !generatedXml.isEmpty();
    }

    @Test(expected = BadRequestException.class)
    public void testGenerateConfigThrowsBadRequestException() {
        List<Jvm> jvmList = new ArrayList<>();
        final Jvm mockJvm = mockJvmWithId(new Identifier<Jvm>(11L));
        jvmList.add(mockJvm);
        when(jvmPersistenceService.findJvms(anyString())).thenReturn(jvmList);
        when(jvmPersistenceService.getJvmTemplate(anyString(), any(Identifier.class))).thenReturn("");
        impl.generateConfigFile("testJvm", "ServerXMLTemplate.tpl");
    }

    @Test
    public void testGetSpecific() {

        final Identifier<Jvm> id = new Identifier<>(-123456L);

        impl.getJvm(id);

        verify(jvmPersistenceService, times(1)).getJvm(eq(id));
    }

    protected Jvm mockJvmWithId(final Identifier<Jvm> anId) {
        final Jvm jvm = mock(Jvm.class);
        when(jvm.getId()).thenReturn(anId);
        return jvm;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGenerateServerXmlConfig() {
        String testJvmName = "testjvm";
        List<Jvm> jvmList = new ArrayList<>();
        jvmList.add(new Jvm(new Identifier<Jvm>(99L), "testJvm", new HashSet<Group>()));
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(jvmList);
        String expectedValue = "<server>xml-content</server>";
        when(jvmPersistenceService.getJvmTemplate(anyString(), any(Identifier.class))).thenReturn(expectedValue);

        // happy case
        String serverXml = impl.generateConfigFile(testJvmName, "server.xml");
        assertEquals(expectedValue, serverXml);

        // return too many jvms
        jvmList.add(new Jvm(new Identifier<Jvm>(999L), "testJvm2", new HashSet<Group>()));
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(jvmList);
        boolean isBadRequest = false;
        try {
            impl.generateConfigFile(testJvmName, "server.xml");
        } catch (BadRequestException e) {
            isBadRequest = true;
        }
        assertTrue(isBadRequest);

        // return no jvms
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(new ArrayList<Jvm>());
        isBadRequest = false;
        try {
            impl.generateConfigFile(testJvmName, "server.xml");
        } catch (BadRequestException e) {
            isBadRequest = true;
        }
        assertTrue(isBadRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGenerateContextXmlConfig() {
        String testJvmName = "testjvm";
        List<Jvm> jvmList = new ArrayList<>();
        jvmList.add(new Jvm(new Identifier<Jvm>(99L), "testJvm", new HashSet<Group>()));
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(jvmList);
        String expectedValue = "<server>xml-content</server>";
        when(jvmPersistenceService.getJvmTemplate(anyString(), any(Identifier.class))).thenReturn(expectedValue);

        // happy case
        String serverXml = impl.generateConfigFile(testJvmName, "server.xml");
        assertEquals(expectedValue, serverXml);

        // return too many jvms
        jvmList.add(new Jvm(new Identifier<Jvm>(999L), "testJvm2", new HashSet<Group>()));
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(jvmList);
        boolean isBadRequest = false;
        try {
            impl.generateConfigFile(testJvmName, "server.xml");
        } catch (BadRequestException e) {
            isBadRequest = true;
        }
        assertTrue(isBadRequest);

        // return no jvms
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(new ArrayList<Jvm>());
        isBadRequest = false;
        try {
            impl.generateConfigFile(testJvmName, "server.xml");
        } catch (BadRequestException e) {
            isBadRequest = true;
        }
        assertTrue(isBadRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGenerateSetenvBatConfig() {
        String testJvmName = "testjvm";
        List<Jvm> jvmList = new ArrayList<>();
        jvmList.add(new Jvm(new Identifier<Jvm>(99L), "testJvm", new HashSet<Group>()));
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(jvmList);
        String expectedValue = "<server>xml-content</server>";
        when(jvmPersistenceService.getJvmTemplate(anyString(), any(Identifier.class))).thenReturn(expectedValue);

        // happy case
        String serverXml = impl.generateConfigFile(testJvmName, "server.xml");
        assertEquals(expectedValue, serverXml);

        // return too many jvms
        jvmList.add(new Jvm(new Identifier<Jvm>(999L), "testJvm2", new HashSet<Group>()));
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(jvmList);
        boolean isBadRequest = false;
        try {
            impl.generateConfigFile(testJvmName, "server.xml");
        } catch (BadRequestException e) {
            isBadRequest = true;
        }
        assertTrue(isBadRequest);

        // return no jvms
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(new ArrayList<Jvm>());
        isBadRequest = false;
        try {
            impl.generateConfigFile(testJvmName, "server.xml");
        } catch (BadRequestException e) {
            isBadRequest = true;
        }
        assertTrue(isBadRequest);
    }

    @Test
    public void testPerformDiagnosis() throws IOException, URISyntaxException {
        Identifier<Jvm> aJvmId = new Identifier<>(11L);
        Jvm jvm = mock(Jvm.class);
        when(jvm.getId()).thenReturn(aJvmId);
        when(jvm.getStatusUri()).thenReturn(new URI("http://test.com"));
        when(jvmPersistenceService.getJvm(aJvmId)).thenReturn(jvm);

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockClientFactoryHelper.requestGet(any(URI.class))).thenReturn(mockResponse);

        String diagnosis = impl.performDiagnosis(aJvmId);
        assertTrue(!diagnosis.isEmpty());
    }

    @Test
    public void testGetResourceTemplateNames() {
        String testJvmName = "testJvmName";
        ArrayList<String> value = new ArrayList<>();
        when(jvmPersistenceService.getResourceTemplateNames(testJvmName)).thenReturn(value);
        value.add("testJvm.tpl");
        List<String> result = impl.getResourceTemplateNames(testJvmName);
        assertTrue(result.size() == 1);
    }

    @Test
    public void testGetResourceTemplate() {
        String testJvmName = "testJvmName";
        String resourceTemplateName = "test-resource.tpl";
        Jvm jvm = mock(Jvm.class);
        String expectedValue = "<template>resource</template>";
        when(jvmPersistenceService.getResourceTemplate(testJvmName, resourceTemplateName)).thenReturn(expectedValue);
        List<Jvm> jvmList = new ArrayList<>();
        jvmList.add(jvm);
        when(jvmPersistenceService.findJvms(testJvmName)).thenReturn(jvmList);
        String result = impl.getResourceTemplate(testJvmName, resourceTemplateName, true);
        assertEquals(expectedValue, result);
    }

    @Test
    public void testUpdateResourceTemplate() {
        String testJvmName = "testJvmName";
        String resourceTemplateName = "test-resource.tpl";
        String template = "<template>update</template>";
        when(jvmPersistenceService.updateResourceTemplate(testJvmName, resourceTemplateName, template)).thenReturn(template);
        String result = impl.updateResourceTemplate(testJvmName, resourceTemplateName, template);
        assertEquals(template, result);
    }

    @Test
    public void testGenerateInvokeBat() {
        final Jvm jvm = mock(Jvm.class);
        final List<Jvm> jvms = new ArrayList<>();
        jvms.add(jvm);
        when(jvmPersistenceService.findJvms(anyString())).thenReturn(jvms);
        when(jvmPersistenceService.getJvms()).thenReturn(jvms);
        when(fileManager.getResourceTypeTemplate(anyString())).thenReturn("template contents");
        final String result = impl.generateInvokeBat(anyString());
        assertEquals("template contents", result);
    }

    @Test
    public void testGetJpaJvm() {
        impl.getJpaJvm(new Identifier<Jvm>(1L), false);
        verify(jvmPersistenceService).getJpaJvm(new Identifier<Jvm>(1L), false);
    }

    @Test
    public void testGetJvmByName() {
        List<Jvm> jvmList = new ArrayList<>();
        jvmList.add(mockJvmWithId(new Identifier<Jvm>(99L)));
        when(jvmPersistenceService.findJvms(anyString())).thenReturn(jvmList);
        impl.getJvm("testJvm");
        verify(jvmPersistenceService).findJvms("testJvm");
    }

}
