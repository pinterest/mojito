UPDATE ai_prompt SET prompt_top_p = 1 WHERE prompt_top_p = 0;
ALTER TABLE ai_prompt ALTER COLUMN prompt_top_p SET DEFAULT 1;
