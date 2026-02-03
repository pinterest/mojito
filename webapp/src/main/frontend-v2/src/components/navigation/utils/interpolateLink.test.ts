import { describe, expect, it } from "vitest";
import { interpolateLink } from "./interpolateLink";

describe("interpolateLink", () => {
  it("should handle empty template and empty variables", () => {
    const template = "";
    const variables = {};
    const result = interpolateLink(template, variables);
    expect(result).toBe("");
  });

  it("should return original template when no variables to replace", () => {
    const template = "/static/path";
    const variables = {};
    const result = interpolateLink(template, variables);
    expect(result).toBe("/static/path");
  });

  it("should not replace value if names are not exactly equal", () => {
    const template = "${branchName}";
    const variables = { branch: "main" };
    const result = interpolateLink(template, variables);
    expect(result).toBe("");
  });

  it("should ignore invalid variable syntax", () => {
    const template = "/users/${userId} and $invalidSyntax";
    const variables = { userId: "john123", invalidSyntax: "ignored" };
    const result = interpolateLink(template, variables);
    expect(result).toBe("/users/john123 and $invalidSyntax");
  });

  it("should replace single variable with string value", () => {
    const template = "/users/${userId}";
    const variables = { userId: "john123" };
    const result = interpolateLink(template, variables);
    expect(result).toBe("/users/john123");
  });

  it("should replace single variable with number value", () => {
    const template = "/posts/${postId}";
    const variables = { postId: 42 };
    const result = interpolateLink(template, variables);
    expect(result).toBe("/posts/42");
  });

  it("should replace multiple variables", () => {
    const template = "/users/${userId}/posts/${postId}";
    const variables = { userId: "john123", postId: 42 };
    const result = interpolateLink(template, variables);
    expect(result).toBe("/users/john123/posts/42");
  });

  it("should handle missing variables by replacing with empty string", () => {
    const template = "/users/${userId}/posts/${postId}";
    const variables = { userId: "john123" };
    const result = interpolateLink(template, variables);
    expect(result).toBe("/users/john123/posts/");
  });

  it("should replace same variable multiple times", () => {
    const template = "/users/${id}/profile/${id}";
    const variables = { id: "123" };
    const result = interpolateLink(template, variables);
    expect(result).toBe("/users/123/profile/123");
  });

  it("should handle variables with zero values", () => {
    const template = "/items/${count}";
    const variables = { count: 0 };
    const result = interpolateLink(template, variables);
    expect(result).toBe("/items/0");
  });
});
