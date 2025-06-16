import PostsSection from '@/components/PostsSection';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import LandingImages from '@/components/LandingImages';
const queryClient = new QueryClient();

export default function LandingScreen() {
    return (
        
        <QueryClientProvider client={queryClient}>
            
                <LandingImages/>
                <PostsSection/>
            
    </QueryClientProvider>
    );
}