require 'spec_helper'
require 'pry'

feature 'Manage users', type: :feature do

  context 'an admin user and a bunch of users' do

    let :sign_in_as_admin do
      visit '/'
      fill_in 'email', with: @admin.email
      fill_in 'password', with: @admin.password
      click_on 'Sign in'
    end


    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      sign_in_as_admin
    end


    describe 'searching and filtering users' do

      before :each do
        click_on 'Admin'
        click_on 'Users'

        # set 100 per page so we see all
        select '100', from: 'Per page'
        wait_until { not page.has_content? "Please wait" }
      end

      scenario 'without any filters all admins and users are shown' do
        # we can see all admins and users

        @admins.each do |admin|
          expect(page).to have_content admin.email
        end

        @users.each do |user|
          expect(page).to have_content user.email
        end
      end

      scenario 'filtering by admins' do

        check 'Admins only'

        wait_until { not page.has_content? "Please wait" }

        @admins.each do |admin|
          expect(page).to have_content admin.email
        end

        @users.each do |user|
          expect(page).not_to have_content user.email
        end

      end

      describe 'seaching for a user ' do

        before :each do
          @search_user = @users.first
          @other_users= @users - [@search_user]
        end

        scenario 'searching by email works' do
          fill_in 'Search term', with: @search_user.email
          wait_until { not page.has_content? "Please wait" }

          expect(page).to have_content @search_user.email

          @other_users.each do |other_user|
            expect(page).not_to have_content other_user.email
          end
        end

        scenario 'searching with small spelling error works' do
          fill_in 'Search term', with: "#{@search_user.firstname}X #{@search_user.lastname}"
          wait_until { not page.has_content? "Please wait" }

          expect(page).to have_content @search_user.email

          @other_users.each do |other_user|
            expect(page).not_to have_content other_user.email
          end
        end


      end

    end

  end
end
