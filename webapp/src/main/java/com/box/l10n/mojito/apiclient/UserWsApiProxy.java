package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.apiclient.exception.ResourceNotFoundException;
import com.box.l10n.mojito.apiclient.model.PageUser;
import com.box.l10n.mojito.apiclient.model.Pageable;
import com.box.l10n.mojito.apiclient.model.User;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component("UserWsApiProxyWebapp")
public class UserWsApiProxy {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserWsApiProxy.class);

  @Autowired private UserWsApi userClient;

  public User createUser(User body) throws ResourceNotCreatedException {
    logger.debug("Creating user with username [{}]", body.getUsername());
    try {
      return this.userClient.createUser(body);
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new ResourceNotCreatedException(
            "User with username [" + body.getUsername() + "] already exists");
      } else {
        throw exception;
      }
    }
  }

  public List<User> getUsersByUsername(String username) {
    Pageable pageable = new Pageable();
    pageable.setPage(0);
    pageable.setSize(Integer.MAX_VALUE);
    pageable.setSort(List.of());
    PageUser pageUser = this.userClient.getUsers(pageable, username, null);
    return pageUser.getContent();
  }

  private User getUserByUsername(String username) throws ResourceNotFoundException {
    List<User> users = getUsersByUsername(username);
    if (users.isEmpty()) {
      throw new ResourceNotFoundException("User with username [" + username + "] is not found");
    }
    return users.getFirst();
  }

  public void deleteUserByUsername(String username) throws ResourceNotFoundException {
    logger.debug("Deleting user by username = [{}]", username);
    this.userClient.deleteUserByUserId(this.getUserByUsername(username).getId());
  }

  public void updateUserByUsername(User body, String username) throws ResourceNotFoundException {
    logger.debug("Updating user by username = [{}]", username);
    User userToUpdate = this.getUserByUsername(username);
    userToUpdate.setPassword(body.getPassword());
    userToUpdate.setSurname(body.getSurname());
    userToUpdate.setGivenName(body.getGivenName());
    userToUpdate.setCommonName(body.getCommonName());
    userToUpdate.setAuthorities(body.getAuthorities());

    this.userClient.updateUserByUserId(userToUpdate, userToUpdate.getId());
  }
}
