package com.box.l10n.mojito.cli.command.param;

/**
 * @author wyau
 */
public class Param {

  public static final String BRANCH_LONG = "--branch";
  public static final String BRANCH_SHORT = "-b";
  public static final String BRANCH_DESCRIPTION = "Name of the branch";

  public static final String BRANCH_NAME_LONG = "--name";
  public static final String BRANCH_NAME_SHORT = "-n";
  public static final String BRANCH_NAME_DESCRIPTION =
      "Name of the branch (if none provided it will attempt to remove the \"null\" branch)";

  public static final String BRANCH_NAME_REGEX_LONG = "--name-regex";
  public static final String BRANCH_NAME_REGEX_SHORT = "-nr";
  public static final String BRANCH_NAME_REGEX_DESCRIPTION =
      "Filter the branches by name using a regex. To get all branch not matching \"master\" "
          + "the regex is \"(?!^master$).*\"";

  public static final String BRANCH_TRANSLATED_LONG = "--translated";
  public static final String BRANCH_TRANSLATED_SHORT = "-t";
  public static final String BRANCH_TRANSLATED_DESCRIPTION = "Filter translated branches";

  public static final String BRANCH_NULL_BRANCH_LONG = "--null-branch";
  public static final String BRANCH_NULL_BRANCH_SHORT = "-nb";
  public static final String BRANCH_NULL_BRANCH_DESCRIPTION =
      "Include \"null\" branch when filtering branches by name using a regex (-nr option)";

  public static final String BRANCH_CREATED_BEFORE_LAST_WEEK_LONG = "--created-before-last-week";
  public static final String BRANCH_CREATED_BEFORE_LAST_WEEK_SHORT = "-cblw";
  public static final String BRANCH_CREATED_BEFORE_LAST_WEEK_DESCRIPTION =
      "DEPRECATED: Filter branches that were created before last week";

  public static final String BRANCH_CREATED_BEFORE = "--created-before";
  public static final String BRANCH_CREATED_BEFORE_SHORT = "-cb";
  public static final String BRANCH_CREATED_BEFORE_DESCRIPTION =
      "Filter branches that were created before the given timeframe (D - days, W - weeks, M - months and Y - years, with values from 1 to 999). e.g: 2W";
  public static final String BRANCH_CREATED_BEFORE_OPTIONS_AND_EXAMPLE =
      "(D - days, W - weeks, M - months and Y - years, with values from 1 to 999). e.g: 2W";

  public static final String REPOSITORY_LONG = "--repository";
  public static final String REPOSITORY_SHORT = "-r";
  public static final String REPOSITORY_DESCRIPTION = "Name of the repository";

  public static final String SOURCE_REPOSITORY_LONG = "--source-repository";
  public static final String SOURCE_REPOSITORY_SHORT = "-s";
  public static final String SOURCE_REPOSITORY_DESCRIPTION = "Name of the source repository";

  public static final String TARGET_REPOSITORY_LONG = "--target-repository";
  public static final String TARGET_REPOSITORY_SHORT = "-t";
  public static final String TARGET_REPOSITORY_DESCRIPTION = "Name of the target repository";

  public static final String REPOSITORY_NAME_LONG = "--name";
  public static final String REPOSITORY_NAME_SHORT = "-n";
  public static final String REPOSITORY_NAME_DESCRIPTION =
      "Name of the repository to create or update";

  public static final String REPOSITORY_NEW_NAME_LONG = "--new-name";
  public static final String REPOSITORY_NEW_NAME_SHORT = "-nn";
  public static final String REPOSITORY_NEW_NAME_DESCRIPTION = "New name for the repository";

  public static final String REPOSITORY_DESCRIPTION_LONG = "--description";
  public static final String REPOSITORY_DESCRIPTION_SHORT = "-d";
  public static final String REPOSITORY_DESCRIPTION_DESCRIPTION =
      "Description of the repository to create or update";

  public static final String REPOSITORY_LOCALES_LONG = "--locales";
  public static final String REPOSITORY_LOCALES_SHORT = "-l";
  public static final String REPOSITORY_LOCALES_DESCRIPTION =
      "List of locales to add.  Separated by spaces.  Arrow separated to specify parent language. Bracket enclosed locale will set that locale to be partially translated.  e.g.(\"fr-FR\" \"(fr-CA)->fr-FR\" \"en-GB\" \"(en-CA)->en-GB\" \"en-AU\")";

  public static final String REPOSITORY_LOCALES_MAPPING_LONG = "--locale-mapping";
  public static final String REPOSITORY_LOCALES_MAPPING_SHORT = "-lm";
  public static final String REPOSITORY_LOCALES_MAPPING_DESCRIPTION =
      "Locale mapping, format: \"fr:fr-FR,ja:ja-JP\". "
          + "The keys contain BCP47 tags of the generated files and the values indicate which repository locales are used to fetch the translations.";

  public static final String REPOSITORY_SOURCE_LOCALE_LONG = "--source-locale";
  public static final String REPOSITORY_SOURCE_LOCALE_SHORT = "-sl";
  public static final String REPOSITORY_SOURCE_LOCALE_DESCRIPTION = "Locale of the source strings";

  public static final String SOURCE_DIRECTORY_LONG = "--source-directory";
  public static final String SOURCE_DIRECTORY_SHORT = "-s";
  public static final String SOURCE_DIRECTORY_DESCRIPTION =
      "Directory that contains source assets to be localized";

  public static final String TARGET_DIRECTORY_LONG = "--target-directory";
  public static final String TARGET_DIRECTORY_SHORT = "-t";
  public static final String TARGET_DIRECTORY_DESCRIPTION =
      "Target directory that will contain localized assets";

  public static final String DROP_IMPORT_STATUS = "--import-status";
  public static final String DROP_IMPORT_STATUS_DESCRIPTION =
      "Override the status of translations being imported";

  public static final String SOURCE_LOCALE_LONG = "--source-locale";
  public static final String SOURCE_LOCALE_SHORT = "-sl";
  public static final String SOURCE_LOCALE_DESCRIPTION =
      "Override the default source locale of the filter (if -ft option is used)";

  public static final String FILE_TYPE_LONG = "--file-type";
  public static final String FILE_TYPE_SHORT = "-ft";
  public static final String FILE_TYPE_DESCRIPTION =
      "File type(s), can be a list. If none provided it will scan for default formats: XLIFF, XCODE_XLIFF, MAC_STRING, MAC_STRINGSDICT, ANDROID_STRINGS, PROPERTIES, PROPERTIES_NOBASENAME, PROPERTIES_JAVA, RESW, RESX, PO, XTB, YAML";

  public static final String FILTER_OPTIONS_LONG = "--filter-options";
  public static final String FILTER_OPTIONS_SHORT = "-fo";
  public static final String FILTER_OPTIONS_DESCRIPTION =
      "Filter options, can be a list. Format: \"option1=value1\" \"option2=value2\"";

  public static final String SOURCE_REGEX_LONG = "--source-regex";
  public static final String SOURCE_REGEX_SHORT = "-sr";
  public static final String SOURCE_REGEX_DESCRIPTION =
      "Regular expression to match the path of source assets to localize";

  public static final String DIR_PATH_INCLUDE_PATTERNS_LONG = "--dir-path-include-patterns";
  public static final String DIR_PATH_INCLUDE_PATTERNS_DESCRIPTION =
      "List of patterns of directories to be included. The wildcard '*' can be used to match any sub "
          + "path (only one level, no recursion). For example, 'modules/*/src' will scan "
          + "'modules/1/src/', 'modules/2/src' but not 'modules/1/a/src'";

  public static final String DIR_PATH_EXCLUDE_PATTERNS_LONG = "--dir-path-exclude-patterns";
  public static final String DIR_PATH_EXCLUDE_PATTERNS_DESCRIPTION =
      "List of patterns of directories to be excluded. The wildcard '*' can be used to match any sub "
          + "path (only one level, no recursion). For example, 'modules/*/src' will scan "
          + "'modules/1/src/', 'modules/2/src' but not 'modules/1/a/src'";

  public static final String USERNAME_LONG = "--username";
  public static final String USERNAME_SHORT = "-un";
  public static final String USERNAME_DESCRIPTION = "Username of user";

  public static final String PASSWORD_LONG = "--password";
  public static final String PASSWORD_SHORT = "-pw";
  public static final String PASSWORD_DESCRIPTION = "Prompt for user password";

  public static final String SURNAME_LONG = "--surname";
  public static final String SURNAME_SHORT = "-sn";
  public static final String SURNAME_DESCRIPTION = "Surname of user";

  public static final String GIVEN_NAME_LONG = "--given-name";
  public static final String GIVEN_NAME_SHORT = "-gn";
  public static final String GIVEN_NAME_DESCRIPTION = "Given name of user";

  public static final String COMMON_NAME_LONG = "--common-name";
  public static final String COMMON_NAME_SHORT = "-cn";
  public static final String COMMON_NAME_DESCRIPTION = "Common name of user";

  public static final String ROLE_LONG = "--role";
  public static final String ROLE_SHORT = "-r";
  public static final String ROLE_DESCRIPTION = "Available user roles: PM, TRANSLATOR, ADMIN, USER";

  public static final String EXPORT_LOCALES_LONG = "--locales";
  public static final String EXPORT_LOCALES_SHORT = "-l";
  public static final String EXPORT_LOCALES_DESCRIPTION =
      "List of locales to be exported, format: fr-FR,ja-JP";

  public static final String CHECK_SLA_LONG = "--check-sla";
  public static final String CHECK_SLA_SHORT = "-cs";
  public static final String CHECK_SLA_DESCRIPTION = "whether check sla or not for repository";

  public static final String EXTRACTION_NAME_LONG = "--name";
  public static final String EXTRACTION_NAME_SHORT = "-n";
  public static final String EXTRACTION_NAME_DESCRIPTION = "The name of the extraction";

  public static final String EXTRACTION_OUTPUT_LONG = "--output-directory";
  public static final String EXTRACTION_OUTPUT_SHORT = "-o";
  public static final String EXTRACTION_OUTPUT_DESCRIPTION =
      "The output directory of the extractions commands (default .mojito/extractions)";

  public static final String EXTRACTION_INPUT_LONG = "--input-directory";
  public static final String EXTRACTION_INPUT_SHORT = "-i";
  public static final String EXTRACTION_INPUT_DESCRIPTION =
      "The input directory of the extractions commands (if not specified, uses the output directory)";

  public static final String SIMPLE_FILE_EDITOR_OUTPUT_LONG = "--output-directory";
  public static final String SIMPLE_FILE_EDITOR_OUTPUT_SHORT = "-o";
  public static final String SIMPLE_FILE_EDITOR_OUTPUT_DESCRIPTION = "The output directory";

  public static final String SIMPLE_FILE_EDITOR_INPUT_LONG = "--input-directory";
  public static final String SIMPLE_FILE_EDITOR_INPUT_SHORT = "-i";
  public static final String SIMPLE_FILE_EDITOR_INPUT_DESCRIPTION = "The input directory";

  public static final String PUSH_TYPE_LONG = "--push-type";
  public static final String PUSH_TYPE_DESCRIPTION =
      "To choose the push type. Don't change unless you know exactly what it does";

  public static final String COMMIT_HASH_LONG = "--commit-hash";
  public static final String COMMIT_HASH_SHORT = "-c";
  public static final String COMMIT_HASH_DESCRIPTION =
      "The commit hash that will be used when recording a push-run. Used together with --record-push-run.";
  public static final String COMMIT_CREATE_DESCRIPTION =
      "The commit hash that will be used when recording a commit.";

  public static final String RECORD_PUSH_RUN_LONG = "--record-push-run";
  public static final String RECORD_PUSH_RUN_SHORT = "-rp";
  public static final String RECORD_PUSH_RUN_DESCRIPTION =
      "If passed in, it stores a full list of text units that were processed by the specified commit, through an abstraction called `push run`";
  public static final String AUTHOR_NAME_LONG = "--author-name";
  public static final String AUTHOR_NAME_SHORT = "-n";
  public static final String AUTHOR_NAME_DESCRIPTION = "The name of the author of the commit.";

  public static final String AUTHOR_EMAIL_LONG = "--author-email";
  public static final String AUTHOR_EMAIL_SHORT = "-e";
  public static final String AUTHOR_EMAIL_DESCRIPTION = "The e-mail of the author of the commit.";

  public static final String CREATION_DATE_LONG = "--creation-date";
  public static final String CREATION_DATE_SHORT = "-d";
  public static final String CREATION_DATE_DESCRIPTION =
      "The date when the commit was merged into the mainline branch.";

  public static final String SLACK_NOTIFICATION_CHANNEL_LONG = "--slack-notification-channel";
  public static final String SLACK_NOTIFICATION_CHANNEL_SHORT = "-snc";
  public static final String SLACK_NOTIFICATION_CHANNEL_DESCRIPTION =
      "Slack channel to which notifications are sent, required if sending Slack notifications.";

  public static final String SKIP_MAX_STRINGS_BLOCK_LONG = "--skip-max_strings-block";
  public static final String SKIP_MAX_STRINGS_BLOCK_SHORT = "-smsb";
  public static final String SKIP_MAX_STRINGS_BLOCK_DESCRIPTION =
      "If set to true, it bypasses the checks for the maximum number of strings added and removed.";

  public static final String MAX_STRINGS_ADDED_BLOCK_LONG = "--max-strings-added-block";
  public static final String MAX_STRINGS_ADDED_BLOCK_SHORT = "-msab";
  public static final String MAX_STRINGS_ADDED_BLOCK_DESCRIPTION =
      "The maximum number of strings to be added.";

  public static final String MAX_STRINGS_REMOVED_BLOCK_LONG = "--max-strings-removed-block";
  public static final String MAX_STRINGS_REMOVED_BLOCK_SHORT = "-msrb";
  public static final String MAX_STRINGS_REMOVED_BLOCK_DESCRIPTION =
      "The maximum number of strings to be removed.";

  public static final String CRON_LONG = "--cron";
  public static final String CRON_SHORT = "-c";
  public static final String CRON_DESCRIPTION =
      "Cron expression for the job. It should be in the format of '* * * * *'. e.g. '0 0 * * *' for every day at midnight. ";

  public static final String JOB_TYPE_LONG = "--job-type";
  public static final String JOB_TYPE_SHORT = "-jt";
  public static final String JOB_TYPE_DESCRIPTION =
      "Type of the job to be created. One of 'THIRD_PARTY_SYNC', 'EVOLVE_SYNC";

  public static final String PROPERTIES_STRING_LONG = "--properties-string";
  public static final String PROPERTIES_STRING_SHORT = "-ps";
  public static final String PROPERTIES_STRING_DESCRIPTION =
      "String containing properties for the job in JSON format. "
          + "Example: "
          + "'{\"version\": 1, "
          + "\"thirdPartyProjectId\":\"xxxxxx\","
          + " \"actions\":[\"PULL\", \"PUSH\"]}, "
          + "\"pluralSeparator\":\"|\", "
          + "\"localeMapping\": \"en:en-US, fr:fr-FR\", "
          + "\"skipTextUnitsWithPattern\": \"^skip.*\", "
          + "\"skipAssetsWithPathPattern\": \"^skip/.*\", "
          + "\"includeTextUnitsWithPattern\": \"^include.*\"}'";

  public static final String JOB_UUID_LONG = "--job-uuid";
  public static final String JOB_UUID_SHORT = "-ju";
  public static final String JOB_UUID_DESCRIPTION = "UUID of the job to be updated or deleted";
}
