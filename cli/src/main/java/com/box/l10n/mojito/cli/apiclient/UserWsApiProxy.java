package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.model.PageUser;
import com.box.l10n.mojito.cli.model.Pageable;
import com.box.l10n.mojito.cli.model.User;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class UserWsApiProxy extends UserWsApi {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserWsApiProxy.class);

  public UserWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  @Override
  public User createUser(User body) throws ApiException, CommandException {
    logger.debug("Creating user with username [{}]", body.getUsername());
    try {
      return super.createUser(body);
    } catch (ApiException e) {
      if (e.getCode() == HttpStatus.CONFLICT.value()) {
        throw new CommandException(
            "User with username [" + body.getUsername() + "] already exists");
      } else {
        throw e;
      }
    }
  }

  private User getUserByUsername(String username) throws ApiException, CommandException {
    Pageable pageable = new Pageable();
    pageable.setPage(0);
    pageable.setSize(Integer.MAX_VALUE);
    pageable.setSort(List.of());
    PageUser pageUser = this.getUsers(pageable, username, null);
    if (pageUser.getContent().isEmpty()) {
      throw new CommandException("User with username [" + username + "] is not found");
    }
    return pageUser.getContent().getFirst();
  }

  public void deleteUserByUsername(String username) throws ApiException, CommandException {
    logger.debug("Deleting user by username = [{}]", username);
    this.deleteUserByUserId(this.getUserByUsername(username).getId());
  }

  public void updateUserByUsername(User body, String username) throws ApiException {
    logger.debug("Updating user by username = [{}]", username);
    User userToUpdate = this.getUserByUsername(username);
    userToUpdate.setPassword(body.getPassword());
    userToUpdate.setSurname(body.getSurname());
    userToUpdate.setGivenName(body.getGivenName());
    userToUpdate.setCommonName(body.getCommonName());
    userToUpdate.setAuthorities(body.getAuthorities());

    super.updateUserByUserId(userToUpdate, userToUpdate.getId());
  }
}
