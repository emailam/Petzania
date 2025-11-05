import { schemas } from "./FormSchemas";
import { yupResolver } from "@hookform/resolvers/yup";
import { useForm } from "react-hook-form";
export const useAuthForm = ( schemaName, defaultValues) =>{
    return useForm({
        resolver: yupResolver(schemas[schemaName]),
        mode: "onTouched", // Enable real-time validation
        defaultValues: {
            email: "",
            password:"",
            ...defaultValues 
        }
    })
}