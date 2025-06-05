import * as yup from 'yup';

const emailSchema = yup.string().email('Invalid email').required('Email is required');
const passwordSchema = yup.string()
  .required("Password is required")
  .min(8, "Password must be at least 8 characters")
  .max(30, "Password cannot exceed 30 characters")
  .matches(
    /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>])/,
    "Password must include: 1 uppercase letter, 1 number, and 1 special character"
  ).matches(
    /^\S*$/,
    "Password must not contain spaces"
  );
const usernameSchema = yup.string().required('Username is required').min(5,"Username must be at least 5 characters").max(30, "Password cannot exceed 30 characters").matches(
    /^\S*$/,
    "Username must not contain spaces"
  );
export const schemas = 
    {
    register : yup.object({
    email : emailSchema,
    username: usernameSchema,
    password: passwordSchema,
    confirmPassword: yup.string().oneOf([yup.ref('password'), null], 'Passwords must match').required('Confirm Password is required'),
    termsAccepted: yup.boolean().oneOf([true], 'You must accept the terms and conditions')
}),
    login:yup.object(
        {
        email : emailSchema,
        password: yup.string().required("Enter your password to log in").min(8, "Password must be at least 8 characters").max(30, "Password cannot exceed 30 characters")
        }
    ),
    forgotPassword:yup.object(
        {
        email : emailSchema
        }
    ),
    resetPassword:yup.object(
        {
        password: passwordSchema,
        confirmPassword: yup.string().oneOf([yup.ref('password'), null], 'Passwords must match').required('Confirm Password is required')
        }
    )
};
