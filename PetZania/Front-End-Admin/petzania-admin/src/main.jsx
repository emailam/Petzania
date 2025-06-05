import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AdminProvider } from './context/adminContext.jsx';
import { UserProvider } from './context/userContext.jsx';
import AllAdmins from './Pages/AdminPages/AllAdmins.jsx';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import AllUsers from './Pages/UserPages/AllUsers.jsx';
import RegisterAdmin from './Pages/AdminPages/RegisterAdmin.jsx';
import UserProfile from './Pages/UserPages/UserProfile.jsx';
import LoginPage from './Pages/LoginPage.jsx';
const queryClient = new QueryClient();
const router = createBrowserRouter([
  {
    path:'/',
    element:<LoginPage/>,
    index:true
  }
  ,{
    path:'/login',
    element:<LoginPage/>,
    index:true
  },
  {
    path:'/app',
    element:<App/>,
    children:[
      {
        path:'/app/users',
        element:<AllUsers/>,
        index:true,
      },
      {
        path:'/app/admins',
        element:<AllAdmins/>,
      },
      {
        path:'/app/admins/register',
        element:<RegisterAdmin/>
      },
      {
        path:'/app/users/:userId',
        element:<UserProfile/>
      }
    ]
      
  }
  
])

createRoot(document.getElementById('root')).render(
  <StrictMode>
    
    <QueryClientProvider client={queryClient}>
      <UserProvider>
        <AdminProvider>
          <RouterProvider router = {router}>
          </RouterProvider>
        </AdminProvider>
      </UserProvider>
    </QueryClientProvider>
    
  </StrictMode>,
)
