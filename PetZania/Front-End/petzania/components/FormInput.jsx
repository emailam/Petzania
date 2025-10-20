import { Controller } from 'react-hook-form';
import React from 'react';
import CustomInput from './CustomInput';

export default function FormInput({ control, name, errors, icon, placeholder, maxLength, label, onSubmitEditing, returnKeyType = 'next', inputRef, ...props }) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field }) => (
        <CustomInput
          {...props}
          label={label}
          placeholder={placeholder}
          value={field.value}
          onChangeText={field.onChange}
          onBlur={field.onBlur}
          error={!!errors[name]}
          errorMessage={errors[name]?.message}
          icon={icon}
          maxLength={maxLength}
          returnKeyType={returnKeyType}
          onSubmitEditing={onSubmitEditing}
          inputRef={inputRef}
        />
      )}
    />
  );
}