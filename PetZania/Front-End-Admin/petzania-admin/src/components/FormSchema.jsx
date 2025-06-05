import * as yup from 'yup';
export default function  useSchema (){
    return yup.object({
        username: yup.string()
          .required('Username is required')
          .min(5, "Username must be at least 5 characters")
          .max(30, "Username cannot exceed 30 characters")
          .matches(
            /^\S*$/,
            "Username must not contain spaces"
          ),
        password: yup.string()
          .required("Password is required")
          .min(8, "Password must be at least 8 characters")
          .max(30, "Password cannot exceed 30 characters")
          .matches(
            /^\S*$/,
            "Password must not contain spaces"
          )
      });
} 