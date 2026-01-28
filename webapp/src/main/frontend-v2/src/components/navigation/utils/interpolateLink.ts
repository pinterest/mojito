export function interpolateLink(
    template: string,
    variables: { [key: string]: string | number },
): string {
    let newLink = template;
    const variablesToReplace = newLink.matchAll(/\${(\w+)}/g);
    for (const match of variablesToReplace) {
        const [fullMatch, key] = match;
        const value = variables[key]?.toString() || "";
        newLink = newLink.replace(fullMatch, value);
    }

    return newLink;
}
