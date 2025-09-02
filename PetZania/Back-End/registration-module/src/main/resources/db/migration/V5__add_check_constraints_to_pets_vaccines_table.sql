ALTER TABLE pets_vaccines_urls
    ADD CONSTRAINT check_vaccine_url_length CHECK (vaccine_url IS NULL OR (char_length(vaccine_url) >= 0 AND char_length(vaccine_url) <= 255));