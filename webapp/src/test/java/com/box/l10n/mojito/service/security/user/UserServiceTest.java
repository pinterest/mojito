package com.box.l10n.mojito.service.security.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceTest extends ServiceTestBase {

  @Autowired UserService userService;

  @Autowired UserRepository userRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  public static String serializeObjectToString(Serializable obj) {
    try (ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteArrayOut)) {

      objectOut.writeObject(obj);
      byte[] byteData = byteArrayOut.toByteArray();
      return Base64.getEncoder().encodeToString(byteData);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public static Object deserializeStringToObject(String serializedString) {
    try {
      byte[] byteData = Base64.getDecoder().decode(serializedString);
      try (ByteArrayInputStream byteArrayIn = new ByteArrayInputStream(byteData);
          ObjectInputStream objectIn = new ObjectInputStream(byteArrayIn)) {

        return objectIn.readObject();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  @Test
  public void bla() {
    System.out.println(serializeObjectToString("coucou"));
    String serCoucou = "rO0ABXQABmNvdWNvdQ==";
    System.out.println(deserializeStringToObject(serCoucou));

    final Object desser =
        deserializeStringToObject(
            "rO0ABXNyAC1jb20uYm94LmwxMG4ubW9qaXRvLmVudGl0eS5zZWN1cml0eS51c2VyLlVzZXLRb6d4g87JHwIACUwAC2F1dGhvcml0aWVzdAAPTGphdmEvdXRpbC9TZXQ7TAAKY29tbW9uTmFtZXQAEkxqYXZhL2xhbmcvU3RyaW5nO0wADWNyZWF0ZWRCeVVzZXJ0AC9MY29tL2JveC9sMTBuL21vaml0by9lbnRpdHkvc2VjdXJpdHkvdXNlci9Vc2VyO0wAB2VuYWJsZWR0ABNMamF2YS9sYW5nL0Jvb2xlYW47TAAJZ2l2ZW5OYW1lcQB+AAJMABBwYXJ0aWFsbHlDcmVhdGVkcQB+AARMAAhwYXNzd29yZHEAfgACTAAHc3VybmFtZXEAfgACTAAIdXNlcm5hbWVxAH4AAnhyACpjb20uYm94LmwxMG4ubW9qaXRvLmVudGl0eS5BdWRpdGFibGVFbnRpdHlhSWz0UQcR1AIAAkwAC2NyZWF0ZWREYXRldAAZTGphdmEvdGltZS9ab25lZERhdGVUaW1lO0wAEGxhc3RNb2RpZmllZERhdGVxAH4ABnhyACVjb20uYm94LmwxMG4ubW9qaXRvLmVudGl0eS5CYXNlRW50aXR5WGwXCbGN99wCAAFMAAJpZHQAEExqYXZhL2xhbmcvTG9uZzt4cHNyAA5qYXZhLmxhbmcuTG9uZzuL5JDMjyPfAgABSgAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAAAAAA3NyAA1qYXZhLnRpbWUuU2VylV2EuhsiSLIMAAB4cHcNBgAAB+gDHQQ22wAIAHhzcQB+AA13DQYAAAfoAx0ENtsACAB4c3IAKm9yZy5oaWJlcm5hdGUuY29sbGVjdGlvbi5zcGkuUGVyc2lzdGVudFNldJu5xpfBF85rAgABTAADc2V0cQB+AAF4cgA5b3JnLmhpYmVybmF0ZS5jb2xsZWN0aW9uLnNwaS5BYnN0cmFjdFBlcnNpc3RlbnRDb2xsZWN0aW9uM6SwSjzwRgwCAAtaABthbGxvd0xvYWRPdXRzaWRlVHJhbnNhY3Rpb25JAApjYWNoZWRTaXplWgAFZGlydHlaAA5lbGVtZW50UmVtb3ZlZFoAC2luaXRpYWxpemVkWgANaXNUZW1wU2Vzc2lvbkwAA2tleXQAEkxqYXZhL2xhbmcvT2JqZWN0O0wABW93bmVycQB+ABJMAARyb2xlcQB+AAJMABJzZXNzaW9uRmFjdG9yeVV1aWRxAH4AAkwADnN0b3JlZFNuYXBzaG90dAAWTGphdmEvaW8vU2VyaWFsaXphYmxlO3hwAP////8AAAEAcQB+AAxxAH4ACXQAOWNvbS5ib3gubDEwbi5tb2ppdG8uZW50aXR5LnNlY3VyaXR5LnVzZXIuVXNlci5hdXRob3JpdGllc3BzcgARamF2YS51dGlsLkhhc2hNYXAFB9rBwxZg0QMAAkYACmxvYWRGYWN0b3JJAAl0aHJlc2hvbGR4cD9AAAAAAAAMdwgAAAAQAAAAAXNyADJjb20uYm94LmwxMG4ubW9qaXRvLmVudGl0eS5zZWN1cml0eS51c2VyLkF1dGhvcml0eXt3ahLV2j+CAgADTAAJYXV0aG9yaXR5cQB+AAJMAA1jcmVhdGVkQnlVc2VycQB+AANMAAR1c2VycQB+AAN4cQB+AAVxAH4ADHNxAH4ADXcNBgAAB+gDHQQ22wAIAHhzcQB+AA13DQYAAAfoAx0ENtsACAB4dAAJUk9MRV9VU0VScHBxAH4AGXhzcgARamF2YS51dGlsLkhhc2hTZXS6RIWVlri3NAMAAHhwdwwAAAAQP0AAAAAAAAFxAH4AGXh0AApjb21tb25OYW1lcHNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAF0AAlnaXZlbk5hbWVzcQB+ACAAdAA8JDJhJDEwJGpJUi83ZDlDT2dLS2labDFUUkd0Sk9OSnBaU1NabkoxeHRaU3c3dTBHQ2UwT3VnSEJlZVBXdAAHc3VybmFtZXQAe2NvbS9ib3gvbDEwbi9tb2ppdG8vc2VydmljZS9zZWN1cml0eS91c2VyL1VzZXJTZXJ2aWNlVGVzdC90ZXN0Q3JlYXRlQmFzaWNVc2VyL3Rlc3RVc2VyL2IzNTc2Y2Y2LTg1ZTgtNGVkMC1hMjBkLWY3MTM3ODc0NzQ5YQ==");
    System.out.println(desser);
  }

  @Test
  public void testCreateBasicUser() {

    String username = testIdWatcher.getEntityName("testUser");
    String pwd = "testPwd1234";
    String surname = "surname";
    String givenName = "givenName";
    String commonName = "commonName";
    Role userRole = Role.ROLE_USER;
    String expectedAuthorityName = userService.createAuthorityName(userRole);

    User userWithRole =
        userService.createUserWithRole(
            username, pwd, userRole, givenName, surname, commonName, false);

    User byUsername = userRepository.findByUsername(username);

    User user = userService.getOrCreatePartialBasicUser(username);
    final String userSer = serializeObjectToString(user);
    System.out.println(userSer);
    deserializeStringToObject(userSer);

    assertEquals("ID should be the same", userWithRole.getId(), byUsername.getId());
    assertNotSame("Password should not be plain", pwd, byUsername.getPassword());
    assertFalse("Should have at least one authority", byUsername.getAuthorities().isEmpty());
    assertEquals(
        "Should have user role",
        expectedAuthorityName,
        byUsername.getAuthorities().iterator().next().getAuthority());
    assertEquals(surname, byUsername.getSurname());
    assertEquals(givenName, byUsername.getGivenName());
    assertEquals(commonName, byUsername.getCommonName());
  }

  @Test(expected = IllegalStateException.class)
  public void testCreateUserWithEmptyPassword() {
    String username = "testUser";
    String pwd = "";
    userService.createUserWithRole(username, pwd, Role.ROLE_USER);
  }

  @Test(expected = NullPointerException.class)
  public void testCreateUserWithNullPassword() {
    String username = "testUser";
    String pwd = null;
    userService.createUserWithRole(username, pwd, Role.ROLE_USER);
  }

  @Test
  public void testSystemUserExist() {
    User systemUser = userService.findSystemUser();
    assertNotNull("System user should always been created.", systemUser);
    assertNotNull("System user has a createdByUser", systemUser.getCreatedByUser());
    assertFalse("System user should be disabled", systemUser.getEnabled());
  }
}
