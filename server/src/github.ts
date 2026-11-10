// SPDX-License-Identifier: AGPL-3.0-only
export type GitHubIssue = { number: number; html_url: string; title: string };

export type GitHubAdapterOptions = {
  token: string;
  repository: string;
  fetchImpl?: typeof fetch;
  apiBase?: string;
};

/**
 * Trusted-side adapter. The token must never be embedded in a distributed
 * mobile artifact. This creates a private issue with a link supplied by the
 * server; it does not upload report attachments by guessing an API.
 */
export class GitHubAdapter {
  private readonly fetchImpl: typeof fetch;
  private readonly apiBase: string;

  constructor(private readonly options: GitHubAdapterOptions) {
    this.fetchImpl = options.fetchImpl ?? fetch;
    this.apiBase = options.apiBase ?? "https://api.github.com";
  }

  async createIssue(title: string, body: string): Promise<GitHubIssue> {
    if (!/^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(this.options.repository)) {
      throw new Error("invalid repository");
    }
    const response = await this.fetchImpl(
      `${this.apiBase}/repos/${this.options.repository}/issues`,
      {
        method: "POST",
        headers: {
          authorization: `Bearer ${this.options.token}`,
          accept: "application/vnd.github+json",
          "content-type": "application/json",
          "x-github-api-version": "2022-11-28",
        },
        body: JSON.stringify({ title, body }),
      },
    );
    if (!response.ok) throw new Error(`GitHub issue creation failed: ${response.status}`);
    const data = (await response.json()) as { number: number; html_url: string; title: string };
    return { number: data.number, html_url: data.html_url, title: data.title };
  }
}
