ALTER TABLE pets_pictures_urls
    ADD CONSTRAINT check_picture_url_length CHECK (picture_url IS NULL OR (char_length(picture_url) >= 0 AND char_length(picture_url) <= 255));