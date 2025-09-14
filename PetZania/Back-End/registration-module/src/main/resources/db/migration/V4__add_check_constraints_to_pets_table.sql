ALTER TABLE pets
    ADD CONSTRAINT check_pets_name_length CHECK (char_length(name) >= 1 AND char_length(name) <= 50);

ALTER TABLE pets
    ADD CONSTRAINT check_pets_description_length CHECK (description IS NULL OR char_length(description) <= 255);

ALTER TABLE pets
    ADD CONSTRAINT check_pets_gender_enum CHECK ( gender IN ('MALE', 'FEMALE') );

ALTER TABLE pets
    ADD CONSTRAINT check_pets_breed_length CHECK (char_length(breed) >= 3 AND char_length(breed) <= 32);